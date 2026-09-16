# pknu 도메인 구현 계획

**Goal:** `pknu` 도메인의 CRUD API를 구현한다. `Semester` 1:N `Course` 1:N `Assignment` — 지금까지(hub/study/life)는 전부 1단계 부모-자식이었고 health는 부모-자식이 없었는데, pknu는 처음으로 **2단계 FK 체인**이다 (Course가 Semester를 참조, Assignment가 Course를 참조). hub/study/life/health 구현·리뷰에서 발견된 함정(아래 Known Pitfalls)을 처음부터 반영한다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller를 둔다. Flyway V5(`pknu_semester`, `pknu_course`, `pknu_assignment` 전부 한 파일에)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (89행)

**Branch:** `feature/domain-pknu-4` (이슈 #4 하위 작업, 이슈 #22가 이 도메인 전용 이슈)

## Known Pitfalls (hub/study/life/health에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다.
3. **common 모듈에 spring-test는 이미 있다.** 다시 추가하지 않는다.
4. **create/update 테스트는 ArgumentCaptor로 실제 전달값을 검증한다.** 이름만 검증을 약속하는 tautological 테스트를 피한다.
5. **update()는 요청의 모든 필드(FK 포함)를 실제로 반영해야 한다.** `Course.update()`가 `semester`를, `Assignment.update()`가 `course`를 조용히 무시하면 안 된다.
6. **update()의 "새 부모가 없는 경우"(not-found) 테스트를 처음부터 포함한다.** 2단계라 **Course는 semester not-found, Assignment는 course not-found 둘 다** 필요하다.
7. **숫자/범위가 있는 필드는 처음부터 검증한다.** `Course.credit`은 `@Min(1) @Max(6)`.
8. **FK 컬럼에는 처음부터 인덱스를 만든다.**
9. **통합 테스트는 부모-자식 이동(move) 시나리오까지 포함한다.** 2단계라 **Course를 다른 Semester로, Assignment를 다른 Course로** 이동하는 시나리오 둘 다 넣는다.
10. **테이블명은 처음부터 도메인 접두사를 붙인다.** `pknu_semester`, `pknu_course`, `pknu_assignment`.
11. **로컬 Postgres 볼륨-체크섬 문제.** study/health에서 반복됐다 — 실제 검증 전에 `docker compose down -v`부터 실행한다.
12. **`server/` 디렉토리에서 `./gradlew` 실행.**

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/pknu/semesters`, `/api/pknu/courses`, `/api/pknu/assignments`.
- 스키마 관리는 Flyway. V1(hub)~V4(health)는 이미 사용 중 — 이 계획은 V5를 쓴다.
- 모든 @DataJpaTest는 @Import(JpaAuditingConfig.class), @ActiveProfiles("test") 선언.
- 이 계획을 시작하기 전에 `main`에 hub/study/life/health가 이미 머지되어 있어야 한다.

## 엔티티 설계

- `Semester`(최상위, FK 없음): `name`(필수, 50자, 예 "2026-1학기"), `startDate`(LocalDate, 필수), `endDate`(LocalDate, 필수).
- `Course`(중간, semester FK 필수): `semester`(FK), `name`(필수, 100자), `professor`(선택, 50자), `credit`(Integer, 필수, 1~6).
- `Assignment`(말단, course FK 필수): `course`(FK), `title`(필수, 200자), `dueDate`(LocalDate, 필수), `completed`(Boolean, 필수), `notes`(선택, 500자).

Service 레이어에서 `Course`는 생성/수정 시 `Semester` 존재를 검증, `Assignment`는 생성/수정 시 `Course` 존재를 검증한다 (study의 `StudyProgressService`가 `StudyTopicRepository`를 함께 주입받는 패턴을 그대로 따름).

## Task 목록

1. **Semester/Course/Assignment 엔티티 + Flyway V5 + Repository + DTO** (+ RepositoryTest, DTO Response 매핑 테스트) — hub의 `HubCategory` 패턴(FK 없는 최상위) + study의 `StudyProgress`/`StudyTopic` 패턴(FK 있는 자식) 참고.
2. **Semester/Course/Assignment Service** (+ ServiceTest: create/findById/update/delete, not-found, ArgumentCaptor 검증. Course/Assignment는 부모 not-found 케이스 포함) — `StudyProgressService` 패턴 그대로.
3. **Semester/Course/Assignment Controller** (+ ControllerTest: 성공 케이스 + 검증 실패 400 케이스) — `StudyProgressController`/`StudyTopicController` 패턴 그대로.
4. **통합 테스트** (`DashboardApplicationTests`에 추가): Semester→Course→Assignment 생성 체인, Course를 다른 Semester로 이동, Assignment를 다른 Course로 이동.
5. **실제 Postgres 검증**: `docker compose down -v` → `up` → `bootRun` → curl로 전체 CRUD + 이동 시나리오 + 검증 실패 확인.
6. **devlog 기록** (`server/devlog/2026-09-16.md`에 `---`로 이어서 추가).
7. **PR 생성 + 머지**, 이슈 #4/#22 갱신.
