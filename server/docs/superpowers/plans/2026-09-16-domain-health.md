# health 도메인 CRUD 구현 계획

**Goal:** `health` 도메인의 CRUD API를 구현한다. hub/study/life와 달리 부모-자식 관계가 없는 3개의 완전히 독립된 엔티티(`HealthLog`, `MealRecord`, `WorkoutLog`)를 각각 독립적으로 구현한다 — life의 `ReadingLog`(독립 엔티티) 패턴을 세 번 반복하는 것과 동일하다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller를 둔다. Flyway V4(`health_log`, `health_meal_record`, `health_workout_log` 세 테이블을 한 파일에)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (88행)

**Branch:** `feature/domain-health-4` (이슈 #4 하위 작업, 이슈 #20에서 세부 추적)

## Known Pitfalls (hub/study/life 도메인에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다 — 필요한 스타터는 이미 다 있음.
3. **common 모듈에 `spring-test`가 이미 있다.** 다시 추가할 필요 없음.
4. **create/update 테스트는 `ArgumentCaptor`로 실제 전달값을 검증한다.** 이름만 검증을 약속하는 척하는 테스트 금지.
5. **update()는 요청의 모든 필드를 실제로 반영해야 한다.** 이번 세 엔티티는 FK가 없어 "부모 무시" 버그는 해당 없지만, enum(mealType) 필드가 update 시 조용히 무시되지 않는지는 반드시 검증한다.
6. **숫자/범위가 있는 필드는 처음부터 검증한다.** calories/carbsG/proteinG/fatG/sodiumMg/caloriesBurned/durationMinutes는 음수 불가(`@PositiveOrZero`/`@Positive`), sleepHours는 0~24 범위(`@DecimalMin`/`@DecimalMax`), weightKg는 양수(`@Positive`).
7. **조회 패턴에 필요한 인덱스를 처음부터 만든다.** FK가 없으므로 대신 날짜 컬럼(recordedAt/consumedAt/performedAt)에 인덱스를 건다 — 향후 "이번 주 기록 조회" 같은 분석 파이프라인 쿼리를 이 인덱스가 지원한다.
8. **테이블명은 도메인 접두사를 처음부터 붙인다.** (life에서 리네임 비용을 치른 교훈) `health_log`, `health_meal_record`, `health_workout_log`.
9. **통합 테스트는 세 엔티티 각각의 CRUD를 실제로 검증한다.** 수동 curl 검증에만 의존하지 않는다.
10. **`server/` 디렉토리에서 `./gradlew` 실행.**

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/health/logs`, `/api/health/meal-records`, `/api/health/workout-logs`.
- 스키마 관리는 Flyway. V1(hub), V2(study), V3(life)는 이미 사용 중 — 이 계획은 V4를 쓴다. 운영은 `ddl-auto=validate`.
- 모든 @DataJpaTest는 @Import(JpaAuditingConfig.class) 선언.
- 이 계획을 시작하기 전에 `main`에 hub(PR #12), study(PR #13), life(PR #19)가 이미 머지되어 있어야 한다.

## 엔티티 설계

- `HealthLog`(독립): `recordedAt`(LocalDate, 필수), `weightKg`(Double, 선택, 양수), `sleepHours`(Double, 선택, 0~24), `notes`(선택, 500자).
- `MealRecord`(독립): `consumedAt`(LocalDateTime, 필수), `mealType`(enum `BREAKFAST/LUNCH/DINNER/SNACK`, 필수), `calories`(Integer, 필수, 0 이상), `carbsG`/`proteinG`/`fatG`/`sodiumMg`(Double, 선택, 0 이상), `notes`(선택, 500자).
- `WorkoutLog`(독립): `performedAt`(LocalDate, 필수), `type`(String, 필수, 100자), `durationMinutes`(Integer, 필수, 1 이상), `caloriesBurned`(Integer, 선택, 0 이상), `notes`(선택, 500자).

## 구현 순서 (Task, 각 Task는 커밋 1개 + TDD)

각 Task는 life의 ReadingLog Task와 동일한 순서로 진행한다: (1) 엔티티+Flyway+Repository, (2) DTO, (3) Service(테스트 먼저), (4) Controller(테스트 먼저). 세 엔티티(HealthLog/MealRecord/WorkoutLog)에 대해 반복.

### Task 1: Flyway V4 + 세 엔티티/Repository
`api/src/main/resources/db/migration/V4__health.sql`에 세 테이블 모두 생성 (인덱스 포함). `common`에 `HealthLog`/`MealRecord`/`WorkoutLog` 엔티티(생성자 + `update()` 메서드), `api`에 각 Repository(`JpaRepository<T, Long>`, 추가 쿼리 메서드 없음, life의 ReadingLogRepository와 동일).

**MealRecord의 mealType은 `@Enumerated(EnumType.STRING)` + `@Column(length = 20)`.**

각 Repository에 대해 `@DataJpaTest` 저장/조회 테스트(life의 `ReadingLogRepositoryTest`와 동일 패턴) 3개 작성.

### Task 2: 세 엔티티의 Request/Response DTO
record 기반 DTO, life의 `ReadingLogRequest`/`ReadingLogResponse`와 동일 패턴. Response는 `from(entity)` 정적 팩토리. 각 Response에 대해 `xxxResponseTest`(엔티티→DTO 매핑 검증) 작성.

**검증 어노테이션은 위 "숫자/범위" 규칙을 Request DTO에 그대로 적용.**

### Task 3: 세 엔티티의 Service (+ ServiceTest 먼저)
life의 `ReadingLogService`와 동일한 5개 메서드(create/findAll/findById/update/delete), `EntityNotFoundException` 사용. `ArgumentCaptor`로 create/update의 실제 전달값 검증, not-found 케이스 테스트 포함.

### Task 4: 세 엔티티의 Controller (+ ControllerTest 먼저)
life의 `ReadingLogController`와 동일한 5개 엔드포인트(`POST`/`GET`/`GET/{id}`/`PUT/{id}`/`DELETE/{id}`), `@WebMvcTest` + `@Import(GlobalExceptionHandler.class)` + `addFilters = false`. 검증 실패(400) 케이스 테스트 포함(예: calories 음수, sleepHours 25).

### Task 5: 통합 테스트
`DashboardApplicationTests`에 세 엔티티 각각의 create→list 엔드투엔드 테스트 추가 (life의 `createsAndFetchesReadingLogEndToEnd`와 동일 패턴, `X-API-KEY` 헤더 포함).

### Task 6: 최종 리뷰 + 검증
`./gradlew build` 통과 확인. 가능하면 `docker compose up` + `bootRun`으로 실제 Postgres에 기동해 curl로 3개 엔티티 CRUD 직접 검증. devlog 기록. PR 생성 및 머지.
