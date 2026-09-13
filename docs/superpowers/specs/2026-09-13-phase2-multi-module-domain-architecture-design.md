# Phase 2: 멀티모듈 + 도메인 기반 아키텍처 전환 설계

## 배경

이슈 #1에서 만든 `server/`(단일 모듈, `io.github.junyong.hubdash` 패키지, 계층 기반)는 임시 스캐폴딩이었다. HubDash는 노션 대시보드(HUB, Study, Life, Health, PKNU, Schedule for iOS, Memo)를 개인 서버로 이전하는 백엔드+데이터 포트폴리오 프로젝트이며, 단순 CRUD가 아니라 향후 ETL 파이프라인(Spring Batch)까지 보여주는 것이 목표다. Phase 2는 이 목표 아키텍처(도메인 기반 패키지, 멀티모듈)로 실제 전환하고, 각 도메인의 CRUD 서비스를 만드는 작업이다.

Notion 워크스페이스의 "초기 기획안" 페이지(Dash Board 하위)에 이미 목표 패키지 구조와 도메인별 엔티티 초안이 문서화되어 있으며, 이 스펙은 그 초안을 실제 구현 가능한 범위로 좁힌 것이다.

## 목표

- `server/` 단일 모듈 → Gradle 멀티모듈(`common`, `api`)로 전환, 루트 패키지 `com.junyoung.dashboard`
- 계층 기반 패키지 → 도메인 기반 패키지로 전환
- 8개 도메인(hub, study, life, health, pknu, schedule, memo, user)에 정규화된 엔티티 + REST CRUD API
- 공통 규약(BaseEntity, ApiResponse, GlobalExceptionHandler, API Key 인증) 확립
- Flyway로 스키마 관리

## 범위 밖 (명시적으로 미룸)

- **`batch` 모듈, Spring Batch ETL 파이프라인, Raw 데이터 레이어**: 오늘 사용자가 명시적으로 결정 — "통계/예측 모델은 파이프라인 설계할 때 넣을 모델이라 지금 하지 말고, 지금은 서비스 만드는 초반으로 간다." 배치가 없는데 Raw 테이블을 먼저 만들 이유가 없어 정규화 테이블만 바로 설계한다. `batch` 모듈은 파이프라인 phase에서 추가.
- **`analytics`(통계/예측 모델)**: 위와 동일한 이유로 이번 phase 범위 밖.
- **다중 사용자/회원가입**: 개인 포트폴리오용 single-user 대시보드로 확정 (2026-09-12). `user` 도메인은 로그인/가입 플로우 없이 단일 계정 개념만 가진다.
- **Mac-iOS 클라이언트 동기화 로직**: 클라이언트는 별도 프로젝트. `schedule` 도메인의 `RecurringRule`/동기화(sync) 서브패키지는 이번 phase에서 만들지 않고 `Event` CRUD만 구현한다.

## 아키텍처

### 모듈 구조

원래 계획인 `common/api/batch` 3모듈 중 `batch`는 위 "범위 밖"에 따라 지금 만들지 않는다. 지금 없는 모듈을 미리 만드는 것은 불필요한 선행 구조라 YAGNI에 어긋난다.

```
HubDash/
├── settings.gradle        # include 'common', 'api'
├── common/                # 공유 도메인 (Entity, DTO, BaseEntity)
└── api/                   # REST API (Controller-Service-Repository) + global 설정
```

`server/`(이슈 #1 스캐폴딩)는 이 전환이 끝나면 삭제한다.

### 패키지 구조 (도메인 기반, 루트 `com.junyoung.dashboard`)

```
com.junyoung.dashboard
├── DashboardApplication.java
├── global/
│   ├── config/            # WebConfig, SwaggerConfig 등
│   ├── security/          # ApiKeyAuthFilter
│   ├── exception/         # GlobalExceptionHandler, 커스텀 예외
│   └── common/            # BaseEntity, ApiResponse<T>
└── domain/
    ├── hub/        {controller, service, repository, entity, dto}
    ├── study/      {...}
    ├── life/       {...}
    ├── health/     {...}
    ├── pknu/       {...}
    ├── schedule/   {...}
    ├── memo/       {...}
    └── user/       {...}
```

각 도메인 패키지는 동일한 내부 구조(controller/service/repository/entity/dto)를 따른다.

### 공통 규약

- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt, JPA Auditing) 상속
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일 (성공/에러 포맷 통일)
- 인증: API Key 방식. `ApiKeyAuthFilter`(`OncePerRequestFilter`)가 요청 헤더의 키를 검증. Spring Security 전체 스택 대신 단일 필터로 처리 (single-user라 세션/JWT 불필요)
- API 경로 컨벤션: `/api/{domain}/...`
- 스키마 관리: Flyway (`src/main/resources/db/migration/V1__init.sql` 부터 시작), JPA `ddl-auto`는 `validate`로만 사용

### 도메인별 엔티티

| 도메인 | 엔티티 | 비고 |
|---|---|---|
| hub | `HubCategory`, `HubLink` | 카테고리별 링크/문서 북마크 |
| study | `StudyTopic`, `StudyProgress` | 1(Topic):N(Progress) |
| life | `Habit`, `HabitLog`, `ReadingLog` | Habit 1:N HabitLog |
| health | `HealthLog`(체중/수면), `MealRecord`(칼로리/탄단지/나트륨), `WorkoutLog` | |
| pknu | `Semester`, `Course`, `Assignment` | Semester 1:N Course 1:N Assignment |
| schedule | `Event` | `RecurringRule`은 백로그 (범위 밖 참고) |
| memo | `Memo` | 자유 메모, 제목/본문/태그 |
| user | `UserProfile`, `UserSetting` | single-user 고정 1행 |

## 데이터 흐름

클라이언트 → `ApiKeyAuthFilter` → Controller → Service → Repository(JPA) → Postgres. 응답은 Entity를 DTO로 변환해 `ApiResponse<T>`로 감싸 반환. 에러는 `GlobalExceptionHandler`가 잡아 동일한 `ApiResponse` 포맷(에러 코드/메시지)으로 응답.

## 에러 처리

- 도메인 커스텀 예외(예: `EntityNotFoundException`, `InvalidRequestException`)를 `global/exception`에 정의
- `GlobalExceptionHandler`가 예외 타입별로 HTTP 상태 코드 매핑 후 `ApiResponse.error(...)` 반환
- 검증 실패(`@Valid` 위반)는 400 + 필드별 에러 메시지

## 테스트 전략

- 도메인별 Repository 테스트: `@DataJpaTest` + H2
- 도메인별 Service 테스트: 단위 테스트 (Mockito)
- Controller 테스트: `@WebMvcTest` + MockMvc
- TDD로 진행: 도메인 하나씩 테스트 → 구현 → 통과 확인 → 커밋
- 전체 통합 확인: `./gradlew build`

## 마이그레이션 순서 (구현 계획에서 세분화 예정)

1. 멀티모듈 스캐폴딩(`common`, `api`) + `global` 공통 요소 + Flyway 세팅
2. 도메인 8개를 하나씩 순서대로 구현 (순서는 구현계획에서 결정)
3. 기존 `server/` 삭제, 문서/설정 정리
4. `main`으로 머지

## 미해결/추후 논의

- 도메인 구현 순서 (간단한 것부터? 의존성 있는 것부터?) — writing-plans 단계에서 결정
- Swagger/OpenAPI 문서화 도입 여부 — 백로그
