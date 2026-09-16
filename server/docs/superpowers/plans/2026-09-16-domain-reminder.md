# reminder 도메인 구현 계획

**Goal:** `reminder` 도메인의 `Reminder`(제목, 대상 시각, 대상 도메인/엔티티 느슨한 참조, 발송 여부) 저장+조회 CRUD API를 구현한다. 부모-자식 없는 완전 독립 엔티티(memo/schedule의 `Event`와 동일한 패턴, 1개 엔티티). 실제 발송(이메일/푸시)은 범위 밖 — 저장/조회/수정/삭제까지만. hub~user 구현·리뷰에서 발견된 함정(아래 Known Pitfalls)을 처음부터 반영한다. 이 도메인이 이슈 #4(Phase 2 umbrella)의 마지막 남은 도메인이다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller. Flyway V9(`reminder` 테이블)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다. `targetDomain`/`targetEntityId`는 실제 FK가 아니라 느슨한 참조(어떤 도메인의 어떤 엔티티든 가리킬 수 있어야 하므로 물리적 FK 제약을 걸지 않음).

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (93행)

**Branch:** `feature/domain-reminder-4` (이슈 #4 하위 작업, 이슈 #30이 세부 이슈)

## Known Pitfalls (hub/study/life/health/pknu/schedule/memo/user에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다.
3. **common 모듈에 `spring-test`가 이미 있다.** 다시 추가할 필요 없음.
4. **create/update 테스트는 `ArgumentCaptor`로 실제 전달값을 검증한다.**
5. **update()는 요청의 모든 필드를 실제로 반영한다** (`sent` 필드가 조용히 무시되지 않는지 특히 확인).
6. **필수 문자열 필드는 `@NotBlank`를 쓴다.** `Reminder.title`은 `@NotBlank` (pknu에서 `@NotNull`만 써서 빈 문자열이 통과한 버그가 실제로 있었음).
7. **관련 필드 간 관계 검증이 필요하면 처음부터.** (해당 없음 — `targetDomain`/`targetEntityId`는 서로 독립적인 선택 필드.)
8. **조회 패턴에 필요한 인덱스를 만든다.** `targetAt` 컬럼에 인덱스(발송 대상 조회 시 사용될 패턴 고려).
9. **통합 테스트는 실제 엔드투엔드 CRUD 시나리오를 포함한다.**
10. **`server/` 디렉토리에서 `./gradlew` 실행.**
11. **로컬 Postgres 볼륨 체크섬 불일치 시 `docker compose down -v`로 리셋한다.**
12. **`application-test.properties`는 `spring.flyway.enabled=false`(Hibernate `create-drop`)를 쓴다.** seed 데이터가 필요한 로직이 있으면 마이그레이션 INSERT에 의존하지 말 것 (이번 도메인은 seed 불필요 — 해당 없음).

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/reminder/reminders`.
- 스키마 관리는 Flyway. V1(hub)~V8(user)은 이미 사용 중 — 이 계획은 **V9**을 쓴다. 테이블명은 `reminder` (엔티티가 하나뿐이라 접두사 불필요).
- 모든 `@DataJpaTest`는 `@Import(JpaAuditingConfig.class)` 선언.
- 이 계획을 시작하기 전에 `main`에 hub/study/life/health/pknu/schedule/memo/user가 이미 머지되어 있어야 한다 (확인됨, main 커밋 397bddb).

## 엔티티 설계

- `Reminder`(독립, 부모 없음): `title`(필수, `@NotBlank`, 200자), `targetAt`(LocalDateTime, 필수), `targetDomain`(선택, 50자 — 예 "life", "study" 등 자유 텍스트, 실제 FK 아님), `targetEntityId`(Long, 선택), `sent`(Boolean, 필수).

---

## Task 1: Reminder 엔티티 + Flyway(V9) + Repository + DTO

**Files:**
- `common/.../domain/reminder/entity/Reminder.java`
- `api/src/main/resources/db/migration/V9__reminder.sql`
- `api/.../domain/reminder/repository/ReminderRepository.java`
- `common/.../domain/reminder/dto/ReminderRequest.java`
- `common/.../domain/reminder/dto/ReminderResponse.java`
- `api/src/test/.../domain/reminder/repository/ReminderRepositoryTest.java`
- `common/src/test/.../domain/reminder/dto/ReminderResponseTest.java`

`Reminder`는 `Event`/`Memo`와 동일한 구조(생성자 + `update()` 전체 필드 반영).

## Task 2: ReminderService

**Files:**
- `api/.../domain/reminder/service/ReminderService.java`
- `api/src/test/.../domain/reminder/service/ReminderServiceTest.java`

`EventService`/`MemoService`와 동일한 구조: create/findAll/findById/update/delete, `getOrThrow`로 `EntityNotFoundException`.

## Task 3: ReminderController

**Files:**
- `api/.../domain/reminder/controller/ReminderController.java`
- `api/src/test/.../domain/reminder/controller/ReminderControllerTest.java`

`/api/reminder/reminders` 경로. `EventController`와 동일한 5개 엔드포인트(POST/GET/GET-id/PUT/DELETE). 컨트롤러 테스트는 정상 생성, blank title 400, 목록 조회를 포함.

## Task 4: 통합 테스트

**Files:**
- `api/src/test/.../DashboardApplicationTests.java` (기존 파일에 테스트 메서드 추가)

`createsAndFetchesReminderEndToEnd` — Reminder 생성 후 목록 조회로 실제 저장 확인. (부모-자식이 없으므로 이동 시나리오는 해당 없음.)

## Task 5: 실제 Postgres 검증 + devlog

`docker compose up`(필요시 `down -v`) + `bootRun`으로 V9까지 마이그레이션 적용 확인, curl로 Reminder 전체 CRUD + blank title 400 확인. `server/devlog/2026-09-16.md`에 이어서 기록.

## Task 6: 이슈 #4 마감

reminder 체크박스 + "main 머지" 체크박스 갱신 후 이슈 #4를 close 처리 (Phase 2 umbrella 전체 완료).
