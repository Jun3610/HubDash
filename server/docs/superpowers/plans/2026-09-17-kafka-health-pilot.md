# Kafka 기반 이벤트 ETL 파이프라인 (health 도메인 파일럿)

**Goal:** 지금까지 9개 도메인은 API가 Service→Repository로 바로 정규화 테이블에 쓰는 구조라, 원래 설계(Raw→ETL/정규화→정규화 DB→분석)의 Raw/ETL 개념이 코드에 없었다. health 도메인을 파일럿으로 삼아 "Raw 입력 → Kafka 이벤트 → Consumer가 검증·정규화" 경로를 신규로 추가한다. 기존 HealthLog/MealRecord/WorkoutLog CRUD와 다른 8개 도메인은 건드리지 않는다 — 순수 추가.

**Architecture:** `common` 모듈에 `RawHealthLog` Entity+DTO, `api` 모듈에 Repository/Service(Producer)/Controller/Consumer. Flyway V10으로 `health_raw_log` 테이블 추가. Kafka는 docker-compose에 KRaft 단일 노드로 추가.

**Tech Stack:** 기존(Java 17, Spring Boot 4.1.1, JPA, Flyway, H2 테스트) + `spring-kafka`, `spring-kafka-test`(`@EmbeddedKafka`).

**Branch:** `feature/kafka-health-pilot-36` (이슈 #36)

## Known Pitfalls (hub~reminder + 후속이슈 #14/#15/#16/#18에서 누적됨, 이번에도 반영)

1. Jackson 3버전 (`tools.jackson.databind.ObjectMapper` import) — Kafka JSON (de)serializer 설정 시에도 확인.
2. 테스트 어노테이션 패키지 위치, build.gradle 필요 이상으로 건드리지 않기.
3. 필수 문자열 필드는 `@NotBlank`.
4. 저장/변환 로직은 ArgumentCaptor 등으로 실제 전달값 검증.
5. 숫자/범위 검증 — raw 입력 시엔 검증 안 하지만 Consumer의 ETL 단계에서는 반드시 함 (이번 작업 핵심).
6. `EntityNotFoundException.of(Class, id)` 팩토리 재사용 (이슈 #15), 재구현 금지.
7. 목록 조회가 있다면 `Pageable`/`PageResponse` 컨벤션 재사용 (이슈 #14).
8. `server/` 디렉토리에서 `./gradlew` 실행.
9. 로컬 Postgres 볼륨 체크섬 불일치나면 `docker compose down -v`로 리셋 (Kafka 컨테이너도 같이 내려갔다 올라옴).
10. Kafka Consumer 테스트는 `@EmbeddedKafka`로 실제 publish→consume 흐름 검증.
11. Consumer에서 예외를 던지면 무한 재시도될 수 있음 — "검증 실패=비즈니스 실패"이지 "일시적 장애"가 아니므로, 예외를 던지지 말고 FAILED 상태로 명시적 기록 후 정상 ack.
12. **`FlywayMigrationIntegrationTest`가 마이그레이션 개수(9)와 테이블 목록을 하드코딩하고 있음** — V10 추가 시 이 테스트를 반드시 같이 갱신해야 함 (개수 10, `health_raw_log` 테이블 추가).

## Global Constraints

- 루트 패키지 `com.junyoung.dashboard`, Entity는 `common`, Repository/Service/Controller/Consumer는 `api`.
- `RawHealthLog`도 `BaseEntity` 상속.
- API 경로: `POST /api/health/raw/health-logs`, `GET /api/health/raw/health-logs/{id}`.
- Kafka 토픽: `health-raw-log`. 이벤트 페이로드는 `RawHealthLogCreatedEvent(Long rawHealthLogId)`만 — 이벤트 자체는 가볍게, Consumer가 DB에서 재조회.
- Flyway V10 (V1~V9는 이미 사용 중).
- `application.properties`: `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP:localhost:9092}` 추가.
- `docker-compose.yml`: Kafka 서비스는 `apache/kafka` 공식 이미지, KRaft 단일 노드(Zookeeper 없음).

## 엔티티 설계

`RawHealthLog` (독립, FK 없음):
- `recordedAtRaw`(String, 필수, 100자) — 파싱 전 원본 문자열
- `weightKgRaw`(String, 선택, 50자) — 원본 문자열, 숫자 아닐 수도 있음
- `sleepHoursRaw`(Double, 선택) — **범위 검증 없음** (raw)
- `notes`(String, 선택, 500자)
- `status`(Enum RawStatus: PENDING/PROCESSED/FAILED, 필수)
- `failureReason`(String, 선택, 500자)

Consumer의 ETL 검증 규칙(기존 `HealthLogRequest`와 동일 기준):
- `recordedAtRaw` → `LocalDate.parse()` 실패 시 FAILED
- `weightKgRaw`(있으면) → `Double.parseDouble()` 실패 또는 <= 0 이면 FAILED
- `sleepHoursRaw`(있으면) → 0.0~24.0 범위 아니면 FAILED
- 전부 통과 시 `HealthLog(recordedAt, weightKg, sleepHours, notes)` 생성해 `HealthLogRepository.save()`, `RawHealthLog.markProcessed()`
- 실패 시 `RawHealthLog.markFailed(reason)`

---

### Task 1: 인프라 (docker-compose Kafka, build.gradle, application.properties)

**Files:**
- `server/docker-compose.yml` (수정 — kafka 서비스 추가)
- `server/api/build.gradle` (수정 — spring-kafka, spring-kafka-test 추가)
- `server/api/src/main/resources/application.properties` (수정 — bootstrap-servers 추가)

```yaml
# docker-compose.yml에 추가
  kafka:
    image: apache/kafka:3.9.0
    container_name: hubdash-kafka
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
```

```gradle
// api/build.gradle dependencies에 추가
implementation 'org.springframework.kafka:spring-kafka'
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

```properties
# application.properties에 추가
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP:localhost:9092}
spring.kafka.consumer.group-id=hubdash-health-etl
spring.kafka.consumer.auto-offset-reset=earliest
```

- [ ] `./gradlew build`로 의존성 해석 확인 (아직 코드 없어도 빌드 통과해야 함).

---

### Task 2: RawHealthLog 엔티티 + Flyway V10 + Repository + DTO

**Files:**
- `common/src/main/java/com/junyoung/dashboard/domain/health/entity/RawHealthLog.java`
- `common/src/main/java/com/junyoung/dashboard/domain/health/entity/RawStatus.java` (enum)
- `common/src/main/java/com/junyoung/dashboard/domain/health/dto/RawHealthLogRequest.java`
- `common/src/main/java/com/junyoung/dashboard/domain/health/dto/RawHealthLogResponse.java`
- `api/src/main/resources/db/migration/V10__health_raw_log.sql`
- `api/src/main/java/com/junyoung/dashboard/domain/health/repository/RawHealthLogRepository.java`

`RawStatus`: `PENDING, PROCESSED, FAILED`

`RawHealthLog`:
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_raw_log")
public class RawHealthLog extends BaseEntity {

    @Column(name = "recorded_at_raw", nullable = false, length = 100)
    private String recordedAtRaw;

    @Column(name = "weight_kg_raw", length = 50)
    private String weightKgRaw;

    @Column(name = "sleep_hours_raw")
    private Double sleepHoursRaw;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawHealthLog(String recordedAtRaw, String weightKgRaw, Double sleepHoursRaw, String notes) {
        this.recordedAtRaw = recordedAtRaw;
        this.weightKgRaw = weightKgRaw;
        this.sleepHoursRaw = sleepHoursRaw;
        this.notes = notes;
        this.status = RawStatus.PENDING;
    }

    public void markProcessed() {
        this.status = RawStatus.PROCESSED;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = RawStatus.FAILED;
        this.failureReason = reason;
    }
}
```

`RawHealthLogRequest(String recordedAtRaw /* @NotBlank */, String weightKgRaw, Double sleepHoursRaw, String notes /* @Size(max=500) */)` — **의도적으로 범위/형식 검증 어노테이션 없음** (raw니까 뭐든 받는다). `recordedAtRaw`만 `@NotBlank`.

`RawHealthLogResponse(Long id, String recordedAtRaw, String weightKgRaw, Double sleepHoursRaw, String notes, String status, String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt)` — `status`는 `RawStatus.name()`.

V10 마이그레이션:
```sql
CREATE TABLE health_raw_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    recorded_at_raw VARCHAR(100) NOT NULL,
    weight_kg_raw VARCHAR(50),
    sleep_hours_raw DOUBLE PRECISION,
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_health_raw_log_status ON health_raw_log(status);
```

**Known Pitfall #12 대응**: 이 Task 완료 직후 `FlywayMigrationIntegrationTest`를 갱신 — 마이그레이션 개수 9→10, `expectedTables`에 `"health_raw_log"` 추가.

- [ ] Repository 테스트: save/findById가 정상 동작하는지.
- [ ] `./gradlew build` 통과.

---

### Task 3: Kafka 이벤트 + Producer (Service+Controller)

**Files:**
- `common/src/main/java/com/junyoung/dashboard/domain/health/event/RawHealthLogCreatedEvent.java` (record, `Long rawHealthLogId`)
- `api/src/main/java/com/junyoung/dashboard/global/config/KafkaTopicConfig.java` (토픽 이름 상수 + `NewTopic` 빈, 로컬 개발 편의용 auto-create 대체)
- `api/src/main/java/com/junyoung/dashboard/domain/health/service/RawHealthLogService.java`
- `api/src/main/java/com/junyoung/dashboard/domain/health/controller/RawHealthLogController.java`

`RawHealthLogService.create()`:
```java
@Transactional
public RawHealthLogResponse create(RawHealthLogRequest request) {
    RawHealthLog saved = rawHealthLogRepository.save(new RawHealthLog(
            request.recordedAtRaw(), request.weightKgRaw(), request.sleepHoursRaw(), request.notes()));
    kafkaTemplate.send(TOPIC, new RawHealthLogCreatedEvent(saved.getId()));
    return RawHealthLogResponse.from(saved);
}

public RawHealthLogResponse findById(Long id) {
    return RawHealthLogResponse.from(getOrThrow(id));
}
```

컨트롤러: `POST /api/health/raw/health-logs` → 201, `GET /api/health/raw/health-logs/{id}` → 200. 목록 조회는 이번 스코프에서 불필요(폴링 확인용 단건 조회만).

**중요 (Known Pitfall #4):** create 테스트는 `ArgumentCaptor<RawHealthLogCreatedEvent>`로 `kafkaTemplate.send()`에 실제로 저장된 id가 전달됐는지 검증.

- [ ] Service 테스트(Mockito, KafkaTemplate mock).
- [ ] Controller 테스트.
- [ ] `./gradlew build` 통과.

---

### Task 4: Kafka Consumer (ETL)

**Files:**
- `api/src/main/java/com/junyoung/dashboard/domain/health/consumer/RawHealthLogConsumer.java`

```java
@Component
public class RawHealthLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawHealthLogConsumer.class);

    private final RawHealthLogRepository rawHealthLogRepository;
    private final HealthLogRepository healthLogRepository;

    @KafkaListener(topics = KafkaTopicConfig.HEALTH_RAW_LOG_TOPIC)
    @Transactional
    public void consume(RawHealthLogCreatedEvent event) {
        RawHealthLog raw = rawHealthLogRepository.findById(event.rawHealthLogId()).orElse(null);
        if (raw == null) {
            log.warn("RawHealthLog {} not found, skipping", event.rawHealthLogId());
            return;
        }

        LocalDate recordedAt;
        try {
            recordedAt = LocalDate.parse(raw.getRecordedAtRaw());
        } catch (DateTimeParseException e) {
            raw.markFailed("recordedAtRaw 파싱 실패: " + raw.getRecordedAtRaw());
            return;
        }

        Double weightKg = null;
        if (raw.getWeightKgRaw() != null && !raw.getWeightKgRaw().isBlank()) {
            try {
                weightKg = Double.parseDouble(raw.getWeightKgRaw());
                if (weightKg <= 0) {
                    raw.markFailed("weightKgRaw가 양수가 아님: " + raw.getWeightKgRaw());
                    return;
                }
            } catch (NumberFormatException e) {
                raw.markFailed("weightKgRaw 파싱 실패: " + raw.getWeightKgRaw());
                return;
            }
        }

        Double sleepHours = raw.getSleepHoursRaw();
        if (sleepHours != null && (sleepHours < 0.0 || sleepHours > 24.0)) {
            raw.markFailed("sleepHoursRaw가 범위(0~24) 밖: " + sleepHours);
            return;
        }

        healthLogRepository.save(new HealthLog(recordedAt, weightKg, sleepHours, raw.getNotes()));
        raw.markProcessed();
    }
}
```

**중요 (Known Pitfall #11):** 이 메서드는 예외를 던지지 않는다 — 검증 실패는 전부 `markFailed()`로 흡수하고 정상 리턴(ack). `findById`가 비어있는 방어적 케이스도 예외 대신 로그+리턴.

**중요 (Known Pitfall #10):** 통합 테스트는 `@EmbeddedKafka(partitions = 1, topics = "health-raw-log")`로 실제 publish(`RawHealthLogService.create()` 호출) → Consumer가 처리할 때까지 `Awaitility` 또는 폴링 루프로 대기 → `RawHealthLog.status`가 PROCESSED/FAILED로 바뀌었는지, `HealthLog`가 실제로 생성됐는지 검증. 정상 케이스 1개, 실패 케이스(잘못된 날짜 문자열) 1개 모두 작성.

- [ ] `RawHealthLogConsumerIntegrationTest` (`@EmbeddedKafka`) 작성 — 정상/실패 케이스.
- [ ] `./gradlew build` 통과.

---

### Task 5: 최종 검토 + devlog

- [ ] 위 12가지 Known Pitfalls 재확인.
- [ ] `FlywayMigrationIntegrationTest` 갱신 확인(Task 2에서 이미 했는지 재확인).
- [ ] `./gradlew build` 전체 통과.
- [ ] 실제 `docker compose down -v` → `up`(db+kafka) → `bootRun`으로 정상/실패 케이스 curl 검증.
- [ ] `server/devlog/2026-09-17.md` 작성.
