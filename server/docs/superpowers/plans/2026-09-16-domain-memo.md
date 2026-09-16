# memo 도메인 구현 계획

**Goal:** `memo` 도메인의 `Memo`(자유 메모, 제목/본문/태그) CRUD API를 구현한다. 부모-자식 없는 완전 독립 엔티티(schedule의 `Event`와 동일한 패턴, 1개 엔티티). hub/study/life/health/pknu/schedule 구현·리뷰에서 발견된 함정(아래 Known Pitfalls)을 처음부터 반영한다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller. Flyway V7(`memo` 테이블)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (91행)

**Branch:** `feature/domain-memo-4` (이슈 #4 하위 작업, 이슈 #26이 세부 이슈)

## Known Pitfalls (hub/study/life/health/pknu/schedule에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다.
3. **common 모듈에 `spring-test`가 이미 있다.** 다시 추가할 필요 없음.
4. **create/update 테스트는 `ArgumentCaptor`로 실제 전달값을 검증한다.**
5. **update()는 요청의 모든 필드를 실제로 반영한다.**
6. **필수 문자열 필드는 `@NotBlank`를 쓴다.** `Memo.title`, `Memo.content`는 `@NotBlank`.
7. **관련 필드 간 관계 검증이 필요하면 처음부터 한다.** (해당 없음 — tags는 선택 필드, 제약 없음.)
8. **조회 패턴에 필요한 인덱스를 만든다.** (해당 없음 — memo는 전체 목록 조회만 필요, 별도 인덱스 불필요.)
9. **통합 테스트는 실제 엔드투엔드 CRUD 시나리오를 포함한다.**
10. **`server/` 디렉토리에서 `./gradlew` 실행.**
11. **로컬 Postgres 볼륨 체크섬 불일치 시 `docker compose down -v`로 리셋한다.**

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/memo/memos`.
- 스키마 관리는 Flyway. V1(hub)~V6(schedule)은 이미 사용 중 — 이 계획은 **V7**을 쓴다. 테이블명은 `memo` (엔티티가 하나뿐이라 접두사 불필요, health 같은 어색한 이중 접두사 피함).
- 모든 `@DataJpaTest`는 `@Import(JpaAuditingConfig.class)` 선언.
- 이 계획을 시작하기 전에 `main`에 hub/study/life/health/pknu/schedule이 이미 머지되어 있어야 한다 (확인됨, main 커밋 6fe4a49).

## 엔티티 설계

- `Memo`(독립, 부모 없음): `title`(필수, `@NotBlank`, 200자), `content`(필수, `@NotBlank`, 5000자), `tags`(선택, 300자, comma-separated 문자열 — 별도 태그 테이블/다대다는 이번 스코프에서 과함).

---

## Task 1: Memo 엔티티 + Flyway(V7) + Repository + DTO

**Files:**
- `common/.../domain/memo/entity/Memo.java`
- `api/src/main/resources/db/migration/V7__memo.sql`
- `api/.../domain/memo/repository/MemoRepository.java`
- `common/.../domain/memo/dto/MemoRequest.java`
- `common/.../domain/memo/dto/MemoResponse.java`
- `api/src/test/.../domain/memo/repository/MemoRepositoryTest.java`
- `common/src/test/.../domain/memo/dto/MemoResponseTest.java`

`Memo`는 `Event`와 동일한 구조(생성자 + `update()` 전체 필드 반영).

## Task 2: MemoService

**Files:**
- `api/.../domain/memo/service/MemoService.java`
- `api/src/test/.../domain/memo/service/MemoServiceTest.java`

`EventService`와 동일한 구조: create/findAll/findById/update/delete, `getOrThrow`로 `EntityNotFoundException`.

## Task 3: MemoController

**Files:**
- `api/.../domain/memo/controller/MemoController.java`
- `api/src/test/.../domain/memo/controller/MemoControllerTest.java`

`/api/memo/memos` 경로. `EventController`와 동일한 5개 엔드포인트(POST/GET/GET-id/PUT/DELETE). 컨트롤러 테스트는 정상 생성, blank title 400, blank content 400, 목록 조회를 포함.

## Task 4: 통합 테스트

**Files:**
- `api/src/test/.../DashboardApplicationTests.java` (기존 파일에 테스트 메서드 추가)

`createsAndFetchesMemoEndToEnd` — Memo 생성 후 목록 조회로 실제 저장 확인. (부모-자식이 없으므로 이동 시나리오는 해당 없음.)

## Task 5: 실제 Postgres 검증 + devlog

`docker compose up`(필요시 `down -v`) + `bootRun`으로 V7까지 마이그레이션 적용 확인, curl로 Memo 전체 CRUD + blank title/content 400 확인. `server/devlog/2026-09-16.md`에 이어서 기록.
