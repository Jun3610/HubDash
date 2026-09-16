# schedule 도메인 구현 계획

**Goal:** `schedule` 도메인의 `Event` CRUD API를 구현한다. `RecurringRule`(반복 규칙)과 Mac-iOS 클라이언트 동기화는 스펙(28행)에서 명시적으로 이번 phase 범위 밖 — `Event` 단독 CRUD만 구현한다. hub/study/life/health/pknu 구현·리뷰에서 발견된 함정(아래 Known Pitfalls)을 처음부터 반영한다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller. Flyway V6(`schedule_event` 테이블)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (28행, 90행)

**Branch:** `feature/domain-schedule-4` (이슈 #4 하위 작업, 이슈 #24가 세부 이슈)

## Known Pitfalls (hub/study/life/health/pknu에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다.
3. **common 모듈에 `spring-test`가 이미 있다.** 다시 추가할 필요 없음.
4. **create/update 테스트는 `ArgumentCaptor`로 실제 전달값을 검증한다.** 이름만 검증하는 척하는 테스트를 만들지 않는다.
5. **update()는 요청의 모든 필드를 실제로 반영한다.**
6. **필수 문자열 필드는 `@NotBlank`를 쓴다.** (pknu에서 `@NotNull`만 써서 빈 문자열이 통과하는 버그가 실제로 나왔다.) `Event.title`은 `@NotBlank`.
7. **숫자/날짜 범위가 있는 필드는 처음부터 검증한다.** `Event`는 FK가 없는 대신 `endAt`이 `startAt`보다 이후인지 클래스 레벨(record 메서드 `@AssertTrue`)로 검증한다.
8. **조회 패턴에 필요한 인덱스를 만든다.** FK는 없지만 `startAt` 기준 조회(캘린더 뷰)를 고려해 인덱스를 만든다.
9. **통합 테스트는 실제 엔드투엔드 CRUD 시나리오를 포함한다.**
10. **`server/` 디렉토리에서 `./gradlew` 실행.**
11. **로컬 Postgres 볼륨 체크섬 불일치 시 `docker compose down -v`로 리셋한다.** (health/pknu에서 반복 발생 — 이전 세션의 낡은 볼륨과 마이그레이션 파일 체크섬이 안 맞는 것뿐, 코드 버그 아님.)

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/schedule/events`.
- 스키마 관리는 Flyway. V1(hub)~V5(pknu)는 이미 사용 중 — 이 계획은 **V6**을 쓴다. 테이블명은 `schedule_event` (life에서 배운 도메인 접두사 컨벤션).
- 모든 `@DataJpaTest`는 `@Import(JpaAuditingConfig.class)` 선언.
- 이 계획을 시작하기 전에 `main`에 hub/study/life/health/pknu가 이미 머지되어 있어야 한다 (확인됨, main 커밋 7056054).

## 엔티티 설계

- `Event`(독립, 부모 없음): `title`(필수, `@NotBlank`, 200자), `startAt`(LocalDateTime, 필수), `endAt`(LocalDateTime, 필수, `startAt`보다 이후), `location`(선택, 200자), `description`(선택, 1000자), `allDay`(Boolean, 필수).
- 부모-자식 관계 없음 — health의 독립 엔티티 패턴과 동일하되 1개 엔티티만 존재.

---

## Task 1: Event 엔티티 + Flyway(V6) + Repository + DTO

**Files:**
- `common/.../domain/schedule/entity/Event.java`
- `api/src/main/resources/db/migration/V6__schedule.sql`
- `api/.../domain/schedule/repository/EventRepository.java`
- `common/.../domain/schedule/dto/EventRequest.java` (record, `@AssertTrue`로 `endAt > startAt` 검증)
- `common/.../domain/schedule/dto/EventResponse.java`
- `api/src/test/.../domain/schedule/repository/EventRepositoryTest.java`
- `common/src/test/.../domain/schedule/dto/EventResponseTest.java`

`Event`는 `HealthLog`와 동일한 구조(생성자 + `update()` 전체 필드 반영). `EventRequest`에 `isEndAfterStart()` `@AssertTrue` 메서드로 클래스 레벨 검증 추가 — null 방어(둘 중 하나가 null이면 `@NotNull`이 따로 걸리므로 `@AssertTrue`는 true 반환해 중복 에러 방지).

## Task 2: EventService

**Files:**
- `api/.../domain/schedule/service/EventService.java`
- `api/src/test/.../domain/schedule/service/EventServiceTest.java`

`HealthLogService`와 동일한 구조: create/findAll/findById/update/delete, `getOrThrow`로 `EntityNotFoundException`. update 테스트는 ArgumentCaptor 없이도 응답으로 전체 필드 반영을 검증(HealthLogServiceTest 패턴).

## Task 3: EventController

**Files:**
- `api/.../domain/schedule/controller/EventController.java`
- `api/src/test/.../domain/schedule/controller/EventControllerTest.java`

`/api/schedule/events` 경로. `HealthLogController`와 동일한 5개 엔드포인트(POST/GET/GET-id/PUT/DELETE). 컨트롤러 테스트는 정상 생성, `endAt < startAt` 400, blank title 400, 목록 조회를 포함.

## Task 4: 통합 테스트

**Files:**
- `api/src/test/.../DashboardApplicationTests.java` (기존 파일에 테스트 메서드 추가)

`createsAndFetchesEventEndToEnd` — Event 생성 후 목록 조회로 실제 저장 확인. (부모-자식이 없으므로 이동 시나리오는 해당 없음.)

## Task 5: 실제 Postgres 검증 + devlog

`docker compose up`(필요시 `down -v`) + `bootRun`으로 V6까지 마이그레이션 적용 확인, curl로 Event 전체 CRUD + `endAt<startAt` 400 + blank title 400 확인. `server/devlog/2026-09-16.md`에 이어서 기록.
