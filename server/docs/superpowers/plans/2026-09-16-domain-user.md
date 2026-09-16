# user 도메인 구현 계획

**Goal:** `user` 도메인의 `UserProfile`(프로필), `UserSetting`(설정) 구현. 지금까지(hub~memo)와 다른 패턴: **single-user 고정 1행 싱글톤**. 여러 행을 만들고 목록 조회하는 일반 CRUD가 아니라, 두 엔티티 각각 정확히 1행만 존재하고 조회/수정만 가능하다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller. Flyway V8(`user_profile`, `user_setting` 테이블 스키마만, INSERT 시딩 없음)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md` (92행)

**Branch:** `feature/domain-user-4` (이슈 #4 하위 작업, 이슈 #28이 세부 이슈)

## 싱글톤 설계 결정 (계획 단계에서 확정, 중요)

원래 후보였던 "Flyway 마이그레이션에 INSERT로 기본 행을 미리 심어두기" 방식은 **채택하지 않는다.** 이유: `api/src/test/resources/application-test.properties`에 `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=create-drop`이 이미 설정되어 있어(이슈 #16과 같은 간극 — Flyway 마이그레이션을 실제로 실행하는 테스트가 없음), Flyway INSERT는 **테스트 환경에서 절대 실행되지 않는다.** 운영에서만 시딩되고 테스트에서는 안 되는 두 가지 동작이 갈리는 걸 피하기 위해, 대신 **Service 레이어에서 지연 생성(get-or-create)** 방식을 쓴다:

```java
private UserProfile getOrCreate() {
    List<UserProfile> all = userProfileRepository.findAll();
    if (!all.isEmpty()) return all.get(0);
    return userProfileRepository.save(new UserProfile(DEFAULT_DISPLAY_NAME, null, null));
}
```

- 행이 하나도 없으면(첫 GET 또는 PUT 호출 시) 기본값으로 생성.
- 이미 있으면 그 행(항상 유일해야 함)을 그대로 반환.
- Flyway 마이그레이션은 `CREATE TABLE`만 하고 INSERT는 하지 않는다. 운영/테스트 환경에서 동일하게 동작한다.
- id를 고정값(1L)으로 가정하지 않는다 — `findAll()`에서 첫 번째(유일한) 행을 그대로 쓴다. `BaseEntity`의 `IDENTITY` 전략과 충돌 없음.

## Known Pitfalls (hub~memo에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`. `build.gradle`을 건드리지 않는다.
3. **common 모듈에 `spring-test`가 이미 있다.** 다시 추가할 필요 없음.
4. **create/update 테스트는 `ArgumentCaptor`로 실제 전달값을 검증한다.**
5. **update()는 요청의 모든 필드를 실제로 반영한다.**
6. **필수 문자열 필드는 `@NotBlank`를 쓴다.** `displayName`, `theme`, `language`는 `@NotBlank`. Boolean 필드(`notificationEnabled`)는 life의 `HabitLog.completed` 컨벤션대로 `Boolean` 래퍼 + `@NotNull`.
7. **관련 필드 간 관계 검증이 필요하면 처음부터 한다.** (해당 없음.)
8. **조회 패턴에 필요한 인덱스를 만든다.** (해당 없음 — 각 테이블에 행이 최대 1개뿐.)
9. **통합 테스트는 실제 엔드투엔드 시나리오를 포함한다.** 이번엔 "행이 없는 상태에서 첫 GET 호출 시 기본값으로 자동 생성되는지"와 "PUT으로 수정한 값이 이후 GET에 반영되는지" 둘 다 포함.
10. **`server/` 디렉토리에서 `./gradlew` 실행.**
11. **로컬 Postgres 볼륨 체크섬 불일치 시 `docker compose down -v`로 리셋한다.**

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/user/profile`, `/api/user/settings`. **POST/DELETE/목록조회 없음** — GET(조회)/PUT(수정) 두 개씩만.
- 스키마 관리는 Flyway. V1(hub)~V7(memo)은 이미 사용 중 — 이 계획은 **V8**을 쓴다. 테이블명은 `user_profile`, `user_setting` (bare `user`는 SQL 예약어라 피함).
- 모든 `@DataJpaTest`는 `@Import(JpaAuditingConfig.class)` 선언.
- 이 계획을 시작하기 전에 `main`에 hub/study/life/health/pknu/schedule/memo가 이미 머지되어 있어야 한다 (확인됨, main 커밋 8f3d458).

## 엔티티 설계

- `UserProfile`(싱글톤): `displayName`(필수, `@NotBlank`, 100자, 기본값 "사용자"), `email`(선택, 200자), `bio`(선택, 500자).
- `UserSetting`(싱글톤): `theme`(필수, `@NotBlank`, 20자, 기본값 "LIGHT"), `language`(필수, `@NotBlank`, 10자, 기본값 "ko"), `notificationEnabled`(필수, `Boolean`, 기본값 true).

---

## Task 1: UserProfile 엔티티 + Flyway(V8) + Repository + DTO

**Files:**
- `common/.../domain/user/entity/UserProfile.java`
- `api/src/main/resources/db/migration/V8__user.sql` (UserProfile + UserSetting 테이블 둘 다 이 파일에)
- `api/.../domain/user/repository/UserProfileRepository.java`
- `common/.../domain/user/dto/UserProfileRequest.java`
- `common/.../domain/user/dto/UserProfileResponse.java`
- `api/src/test/.../domain/user/repository/UserProfileRepositoryTest.java`
- `common/src/test/.../domain/user/dto/UserProfileResponseTest.java`

## Task 2: UserSetting 엔티티 + Repository + DTO

**Files:**
- `common/.../domain/user/entity/UserSetting.java`
- `api/.../domain/user/repository/UserSettingRepository.java`
- `common/.../domain/user/dto/UserSettingRequest.java`
- `common/.../domain/user/dto/UserSettingResponse.java`
- `api/src/test/.../domain/user/repository/UserSettingRepositoryTest.java`
- `common/src/test/.../domain/user/dto/UserSettingResponseTest.java`

## Task 3: UserProfileService + UserSettingService

**Files:**
- `api/.../domain/user/service/UserProfileService.java`
- `api/.../domain/user/service/UserSettingService.java`
- `api/src/test/.../domain/user/service/UserProfileServiceTest.java`
- `api/src/test/.../domain/user/service/UserSettingServiceTest.java`

각 Service는 `getOrCreate()`(위 싱글톤 설계 결정 그대로) + `get()`(조회) + `update(request)`(수정, 모든 필드 반영, `ArgumentCaptor`로 검증하는 테스트 포함). 테스트는 "행 없을 때 기본값 생성", "행 있을 때 그대로 반환", "update가 모든 필드 반영" 세 가지 포함.

## Task 4: UserProfileController + UserSettingController

**Files:**
- `api/.../domain/user/controller/UserProfileController.java`
- `api/.../domain/user/controller/UserSettingController.java`
- `api/src/test/.../domain/user/controller/UserProfileControllerTest.java`
- `api/src/test/.../domain/user/controller/UserSettingControllerTest.java`

각각 `GET`/`PUT` 두 엔드포인트만. 컨트롤러 테스트는 정상 조회, 정상 수정, blank displayName/theme/language 400을 포함.

## Task 5: 통합 테스트

**Files:**
- `api/src/test/.../DashboardApplicationTests.java` (기존 파일에 테스트 메서드 추가)

- `getsDefaultUserProfileWhenNotSeededYet` — 아무것도 안 한 상태에서 GET `/api/user/profile` 호출 시 기본값(`displayName: "사용자"`)으로 자동 생성되어 200 응답.
- `updatesUserProfileAndPersists` — PUT으로 수정 후 다시 GET 했을 때 수정된 값이 반영되는지.
- `getsDefaultUserSettingWhenNotSeededYet`, `updatesUserSettingAndPersists` — UserSetting도 동일하게.

## Task 6: 실제 Postgres 검증 + devlog

`docker compose up`(필요시 `down -v`) + `bootRun`으로 V8까지 마이그레이션 적용 확인, curl로 GET(자동 생성 확인)/PUT(수정 반영 확인) × 2 엔티티, blank displayName/theme 400 확인. `server/devlog/2026-09-16.md`에 이어서 기록.
