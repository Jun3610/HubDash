# HubDash 웹 프론트엔드 구현 요청 (React)

너는 시니어 프론트엔드 개발자야. 이미 완성된 Spring Boot REST API 서버(HubDash)에 붙는 **개인용 대시보드 웹 앱**을 React로 만들어줘. 디자인은 확정돼 있고, 아래 명세를 그대로 구현하면 돼. 모르는 값을 지어내지 말고, 서버에 없는 기능은 아래 "서버 제약"대로 처리해.

---

## 0. 결과물과 위치

- 레포 안 `client/web/` 에 새 프로젝트로 만든다. (`server/`는 건드리지 않는다)
- 스택
  - Vite + React 18 + TypeScript (strict)
  - React Router v6 (라우팅)
  - TanStack Query v5 (서버 상태, 캐시, 뮤테이션)
  - 스타일: **CSS 변수(디자인 토큰) + CSS Modules**. Tailwind·UI 킷(MUI, Chakra 등) 쓰지 않는다
  - 차트: 라이브러리 없이 SVG/div로 직접 그린다 (막대, 스파크라인, 히트맵만 필요)
  - 아이콘: `lucide-react` (선 두께 1.7 전후, 16px 기본)
  - 날짜: `date-fns`
- `npm run dev` 한 번으로 뜨고, `npm run build` / `npm run lint` / `npm run typecheck`가 통과해야 한다
- 마지막에 `client/web/README.md`에 실행 방법, 환경 변수, 폴더 구조를 적는다

---

## 1. 서버 연동 규칙 (반드시 지킬 것)

- Base URL: 기본 `http://localhost:8080`. 설정 화면에서 바꿀 수 있고 `localStorage`에 저장
- **모든 요청에 헤더 `X-API-KEY`** (로컬 기본값 `dev-local-key`). 설정 화면에서 입력, `localStorage`에 저장
- 모든 응답은 봉투 형태다
  ```json
  { "success": true,  "data": { ... }, "errorCode": null, "message": null }
  { "success": false, "data": null, "errorCode": "INVALID_REQUEST", "message": "must not be blank" }
  ```
- 목록 응답의 `data`는 페이지 형태: `{ content: T[], page, size, totalElements, totalPages }`
  - 쿼리: `?page=0&size=20&sort=필드,desc` (Spring Pageable, 기본 size 20)
- 날짜 형식: `LocalDate` = `"2026-09-23"`, `LocalDateTime` = `"2026-09-23T15:00:00"` (**시간대 없음**)
  - `LocalDate`를 `new Date("2026-09-23")`로 파싱하면 UTC로 읽혀 하루 밀린다. 반드시 문자열 그대로 다루거나 `date-fns/parseISO`로 로컬 날짜로 파싱
  - `LocalDateTime`의 소수 초 자릿수는 가변(0~9자리)이다
- 에러 처리
  - 401 → 전역 배너 "API 키를 확인해 주세요" + 설정 화면으로 가는 링크
  - 네트워크 실패(서버 꺼짐) → 상단에 "서버에 연결할 수 없어요 · 다시 시도" 배너. 이 서버는 **필요할 때만 로컬에서 켜서 쓰는** 구조라 자주 발생한다. 앱이 죽으면 안 된다
  - `success:false` → `errorCode`와 `message`를 토스트로 그대로 보여준다 (예: `INVALID_REQUEST`, `NOT_FOUND`)
- **CORS**: 서버에 CORS 설정이 없다. 개발 중에는 Vite `server.proxy`로 `/api` → `http://localhost:8080` 프록시를 걸고, 앱은 상대 경로 `/api/...`로 호출한다. Base URL을 바꿨을 때만 절대 경로를 쓰고, 이 경우 CORS가 필요하다는 안내를 README에 적는다
- API 클라이언트는 `src/api/client.ts` 하나로 모으고, 도메인별 함수와 TanStack Query 훅을 `src/api/{domain}.ts`에 둔다. 응답 타입은 아래 명세 그대로 TypeScript 타입으로 정의

---

## 2. API 명세

공통: 아래 리소스는 모두 `POST`(생성) / `GET`(목록, 페이지) / `GET /{id}` / `PUT /{id}` / `DELETE /{id}` CRUD를 가진다. 응답에는 항상 `id, createdAt, updatedAt`이 붙는다.
"필수 쿼리"가 있는 목록은 그 파라미터 없이 호출하면 400이다.

| 리소스 | 경로 | 목록 필수 쿼리 | 요청 필드 (✱ 필수) |
|---|---|---|---|
| 학기 | `/api/pknu/semesters` | – | ✱name, ✱startDate, ✱endDate |
| 과목 | `/api/pknu/courses` | `semesterId` | ✱semesterId, ✱name, professor, ✱credit(1–6) |
| 과제 | `/api/pknu/assignments` | `courseId` | ✱courseId, ✱title, ✱dueDate, ✱completed, notes |
| 공부 주제 | `/api/study/topics` | – | ✱name, description |
| 공부 기록 | `/api/study/progresses` | `topicId` | ✱topicId, ✱studiedAt, ✱minutes(1–1440), notes |
| 습관 | `/api/life/habits` | – | ✱name, description |
| 습관 기록 | `/api/life/habit-logs` | `habitId` | ✱habitId, ✱performedAt, ✱completed, notes |
| 독서 | `/api/life/reading-logs` | – | ✱title, author, ✱startedAt, finishedAt, rating(1–5), notes |
| 끼니 | `/api/health/meal-records` | – | ✱consumedAt(LocalDateTime), ✱mealType(BREAKFAST·LUNCH·DINNER·SNACK), notes |
| 음식 항목 | `/api/health/meal-items` | `mealRecordId` | ✱mealRecordId, ✱name, ✱calories, carbsG, proteinG, fatG, sodiumMg |
| 운동 | `/api/health/workout-logs` | – | ✱performedAt, ✱type, ✱durationMinutes, caloriesBurned, notes |
| 체중·수면 | `/api/health/logs` | – | ✱recordedAt, weightKg, sleepHours(0–24), notes |
| 일정 | `/api/schedule/events` | – | ✱title, ✱startAt, ✱endAt(>startAt), location, description, ✱allDay |
| 메모 | `/api/memo/memos` | – | ✱title, ✱content(≤5000), tags(쉼표 구분 문자열, ≤300) |
| 허브 카테고리 | `/api/hub/categories` | – | ✱name, description |
| 허브 링크 | `/api/hub/links` | `categoryId` | ✱categoryId, ✱title, ✱url, description |
| 리마인더 | `/api/reminder/reminders` | – | ✱title, ✱targetAt(LocalDateTime), targetDomain(≤50), targetEntityId, ✱sent |

응답 전용 필드
- `MealRecordResponse`: `id, consumedAt, mealType, notes, items: MealItemResponse[], totals: MealTotals`
- `MealTotals`: `{ calories, carbsG, proteinG, fatG, sodiumMg }`
- `ReminderResponse`: `id, title, targetAt, targetDomain, targetEntityId, sent`

특수 엔드포인트
- `GET /api/health/meal-records/daily-summary?date=YYYY-MM-DD`
  → `{ date, totals: MealTotals, meals: [{ mealType, itemCount, totals }] }`
- 프로필: `GET /api/user/profile`, `PUT /api/user/profile` — `{ ✱displayName, email, bio }`
- 설정: `GET /api/user/settings`, `PUT /api/user/settings` — `{ ✱theme, ✱language, ✱notificationEnabled }`
- 주간 통계 (조회는 페이지 목록, 재계산은 `POST .../batch-runs` body `{ "weekStart": "YYYY-MM-DD" }` → `{ jobExecutionId, status, weekStart }`)

| 통계 | 조회 경로 | 필수 쿼리 | 응답 필드 |
|---|---|---|---|
| 식단 | `/api/health/analytics/meal-weekly-stats` | – | weekStart, dayCount, avgCalories, avgCarbsG, avgProteinG, avgFatG |
| 체중·수면 | `/api/health/analytics/weekly-stats` | – | weekStart, logCount, avgWeightKg, avgSleepHours |
| 공부 | `/api/study/analytics/topic-weekly-stats` | `topicId` | topicId, weekStart, sessionCount, totalMinutes |
| 습관 | `/api/life/analytics/habit-weekly-stats` | `habitId` | habitId, weekStart, totalCount, completedCount |
| 과제 | `/api/pknu/analytics/assignment-weekly-stats` | `courseId` | courseId, weekStart, totalCount, completedCount, completionRate(0–1) |

`/api/*/raw/...` 엔드포인트(원본 수집용 ETL 입구)는 **프론트에서 쓰지 않는다.**

### 서버 제약과 프론트 처리 방법
- **날짜 범위 필터가 없다.** "오늘 일정", "이번 주 기록" 같은 화면은 목록을 넉넉히 가져와서(`size=200&sort=…,desc`) 프론트에서 거른다. 이 로직은 `src/lib/select/*.ts`에 모아 나중에 서버 필터가 생기면 쉽게 바꿀 수 있게 한다
- **"전체 과제"는 한 번에 못 가져온다.** 학기 → 과목 목록 → 과목별 과제를 병렬 조회(`useQueries`)해서 합친다. 습관 기록(habitId별), 공부 기록(topicId별), 허브 링크(categoryId별)도 같은 방식
- **기록 히트맵(잔디) 전용 API가 없다.** 끼니, 운동, 체중·수면, 공부, 습관, 과제 완료 기록의 날짜를 모아 날짜별 건수로 합산한다
- **강조색은 서버에 저장할 곳이 없다.** `localStorage`에만 저장한다 (theme·language·notificationEnabled는 서버 설정 사용)

---

## 3. 디자인 시스템 (정확히 이 값으로)

전체 느낌: **노션 다크 톤 + 컴팩트한 밀도 + 시그니처 연빨강.** 사이드바만 GitHub 다크 톤.
디자인 원본은 같이 전달한 `screens/*.dc.html`(정확한 스타일 값)과 PDF(시각 참고)다. 읽는 법은 `README.md` 참고. 각 화면을 이 파일들과 최대한 똑같이 구현한다

`src/styles/tokens.css`에 CSS 변수로 정의하고, 컴포넌트에서는 hex를 직접 쓰지 않는다.

```css
:root {
  /* 본문 (노션 다크) */
  --bg: #191919;          --surface: #1e1e1e;      --subtle: #252525;
  --fill: #2c2c2c;        --tag-neutral: #2f2f2f;
  --border: #333333;      --divider: #2a2a2a;
  --text: #e6e6e6;        --text-body: #cfcfcf;    --text-muted: #8f8f8f;

  /* 시그니처 (강조색 — 설정에서 바뀌는 값) */
  --accent: #e27a72;      --accent-strong: #c4554d; --accent-soft: #4a2a27;
  --heat-0: #252525; --heat-1: #3e2624; --heat-2: #6b3530; --heat-3: #9c4540; --heat-4: #e27a72;

  /* 노션 태그색: 글자 / 배경 */
  --blue: #529cca;   --blue-bg: #1b3445;
  --green: #529e72;  --green-bg: #243d30;
  --purple: #9d68d3; --purple-bg: #3c2d49;
  --yellow: #ca9849; --yellow-bg: #4a3d24;
  --orange: #c77d48; --orange-bg: #4f3423;
  --red: #df5453;    --red-bg: #4f2826;

  /* 사이드바 전용 (GitHub 다크) */
  --sb-bg: #0d1117;  --sb-band: #010409;  --sb-active: #212830;
  --sb-border: #3d444d;  --sb-text: #d1d7e0;  --sb-text-strong: #f0f6fc;  --sb-muted: #9198a1;
  --sb-counter: #2f3742;

  --radius: 6px;  --radius-tag: 4px;
  --font-sans: 'IBM Plex Sans KR', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', ui-monospace, monospace;
}
```

- **강조색 프리셋 5개**: 연빨강(기본), 초록, 파랑, 보라, 주황. 프리셋마다 `--accent`, `--accent-strong`, `--accent-soft`, `--heat-1~4`를 한 세트로 정의하고 `<html data-accent="…">`로 전환. 강조색은 버튼, 탭 밑줄, 히트맵, 사이드바 현재 메뉴 막대, 선택 표시에만 쓴다
- **글꼴**: 본문 IBM Plex Sans KR, 숫자·날짜·코드는 JetBrains Mono (Google Fonts). 표의 숫자는 오른쪽 정렬
- **크기**: 기본 13px · 보조 12px · 카드 제목 14/600 · 페이지 제목 20–26/600 · KPI 숫자 mono 20–26/500
- **밀도**: 행 높이 28–32px, 카드 안쪽 여백 12–14px, 카드 간격 10–16px. 여유롭게 띄우지 말고 컴팩트하게
- **모서리**: 카드·버튼 6px, 태그 4px, 카운터 알약 10px
- **카드**: 테두리 1px `--border`, 배경 `--bg` 또는 `--surface`, 그림자 없음. 목록형 카드는 헤더 줄만 `--subtle` 배경
- **태그·라벨·D-day**: 테두리 없이 옅은 배경 + 같은 계열 글자 (`--blue-bg` / `--blue` 등). D-1 이하 빨강, D-3 이하 노랑, 그 외 회색, 완료는 보라
- **버튼**: 기본(생성) = `--accent-strong` 배경 + 흰 글자 · 보조 = `--fill` 배경 + 테두리 · 삭제 = 보조 모양에 `--red` 글자 · 높이 28px
- **탭**: 밑줄형. 현재 탭은 굵은 글씨 + 2px `--accent` 밑줄 + 옆에 카운터 알약
- 그라데이션, 그림자, 이모지, 좌측 색 테두리 카드는 쓰지 않는다

---

## 4. 레이아웃과 공통 컴포넌트

- 데스크톱: 왼쪽 사이드바 232px 고정 + 본문. 본문 상단 44px 헤더(브레드크럼 `HubDash / 화면명`, 탭, 오른쪽 주요 버튼)
- **사이드바 (GitHub 톤 + 커스텀)**
  - 상단 52px 띠(`--sb-band`): 연빨강 사각 로고(H) + `Jun3610 / HubDash`(뒤쪽 굵게) + 작업 공간 메뉴 버튼
  - 검색 버튼: `/` 키 모양 표시 + `⌘K` (누르면 전역 검색 팝업 — 메모 제목, 허브 링크, 과제 제목 대상)
  - "이번 주 기록" 카드: 월~일 7칸 히트맵, 오늘은 외곽선, 미래 요일은 흐리게, 오른쪽에 주간 건수, 아래 "N일 연속 기록 중"
  - 메뉴 9개: 홈, 허브(카운터), 공부, 학업·PKNU(미완료 과제 수), 건강, 생활·습관, 일정, 메모, 리마인더(대기 수, 강조색 알약). 현재 메뉴는 `--sb-active` 배경 + 왼쪽 4px 강조색 막대
  - 섹션 "현재 학기"(접기 가능): 과목별 색 점 + 이름 + 가장 가까운 과제 D-day
  - 섹션 "고정한 링크": 허브 링크 몇 개 (고정 여부는 서버에 없으니 `localStorage`)
  - 하단 띠: 이니셜 아바타(온라인 점) + displayName + `@Jun3610` + 설정 버튼, 그 아래 `서버 주소 · API 연결됨/끊김` 상태 점
- 모바일(<768px): 사이드바 숨김, 상단 헤더(날짜 + 검색 + 빠른 기록 버튼, 터치 영역 44px), 하단 탭 5개(홈, 건강, 학업, 허브, 더보기→설정)
- 공통 컴포넌트: `Card`, `SectionHeader`, `Tabs`, `Tag`, `Counter`, `DdayBadge`, `ProgressBar`, `Heatmap`, `BarChart`, `Sparkline`, `KpiTile`, `Button`, `IconButton`, `Field`(label + input/select/textarea + 오류 메시지), `Checkbox`, `Switch`, `Segmented`, `Toast`, `Banner`, `EmptyState`, `Skeleton`, `ConfirmDialog`, `Modal`
- 모든 폼: `label` 연결, 서버 검증 규칙(위 표의 필수·범위)을 프론트에서도 먼저 검사, 서버 `message`가 오면 해당 필드 아래 빨간 글씨로 표시
- 상태 화면: 불러오는 중 = 회색 스켈레톤 줄 / 빈 목록 = "아직 기록이 없어요" + 추가 버튼 / 401·네트워크 오류 = 위 배너

---

## 5. 화면 (라우트)

| 라우트 | 화면 | 핵심 구성 |
|---|---|---|
| `/` | 홈 | 프로필 한 줄(아바타, 이름, bio, 태그 3개) · KPI 4칸(오늘 섭취 kcal/목표, 단백질/목표, 이번 주 공부 시간, 이번 주 과제 완료율) · 최근 1년 기록 히트맵(53주×7, 연도 전환, 범례) + 활동 개요(도메인별 비율 막대) · 오늘 일정 · 과제(D-day, 체크로 완료 토글) · 오늘 습관(체크 + 이번 주 7점) · 오늘 식단(칼로리·단백질·탄수·지방·나트륨 목표 대비 막대 + 아침/점심/저녁/간식 요약) · 리마인더 · HUB 카테고리 3×3 · 최근 메모 |
| `/health` | 건강 | 탭: 식단·운동·체중수면·주간통계 · 날짜 이동(◀ 오늘 ▶) · daily-summary 합계 + 매크로 막대 · 끼니별 카드(음식 표: 음식, kcal, 탄, 단, 지, 나트륨 / 음식 추가 / 기록 없음 상태) · 오른쪽: 주간 평균 칼로리 막대 8주(목표선 1,500) · 운동 목록 · 체중·수면 스파크라인 |
| `/pknu` | 학업 | 학기 탭 · 학기 요약(기간, N주차, 학점 합계) · 과목 표(과목, 교수, 학점, 과제 진행 막대, 다음 마감) · 과제 목록(GitHub Issues처럼 "N 진행 중 / N 완료", 과목 라벨, D-day) · 주간 과제 완료율 4칸 + "통계 다시 계산" |
| `/study` | 공부 | 이번 주 KPI 4칸 · 주제 카드 3열(최근 8주 막대, 이번 주 시간, 세션 수) · 최근 기록 표 · 오른쪽: 빠른 기록 폼(주제, 날짜, 분 + 25/50/90분 버튼, 메모) · 이번 주 주제별 가로 막대 |
| `/life` | 생활·습관 | 학기 다짐 태그 줄(설정에서 편집, `localStorage`) · 습관 트래커(습관 × 최근 14일 칸, 칸 클릭 = 그날 habit-log 생성/토글, 오른쪽에 이번 주 달성·연속일) · 독서 표(상태 태그, 기간, 별점) · 오른쪽: 오늘 체크 · 주간 달성률 막대 · 읽는 중인 책 |
| `/schedule` | 일정 | 월/주/목록 전환(주 보기 기본) · 이전/오늘/다음 · 종일 줄 · 08–22시 시간 격자 · 오늘 열 강조 + 현재 시각선 · 일정 블록 클릭 → 오른쪽 상세(시간, 장소, 종일, 메모, 수정/삭제, 연결된 리마인더) · 다가오는 일정 7일 |
| `/memo` | 메모 | 2단: 목록(검색, 태그 필터, 제목·수정일·미리보기·태그) / 편집기(제목, 태그 편집, 편집·미리보기 전환, 본문은 마크다운으로 저장하고 미리보기는 렌더링, 자동 저장 표시 "저장됨") |
| `/hub` | 허브 | 2단: 카테고리 목록(개수, "전체") / 링크 카드 3열(첫 글자 아이콘, 제목, 도메인 mono, 설명, 추가일) · 카드/목록 전환 · 링크 추가 폼(주소, 제목, 카테고리, 설명) · 링크 클릭 시 새 탭 |
| `/reminders` | 리마인더 | 탭: 대기·보냄·전체 · 묶음: "지남 · 아직 안 보냄" / "오늘" / "예정" · 행: 시각, 제목, targetDomain 태그 + 대상 링크(해당 화면으로 이동), 보냄 처리/미루기 · 오른쪽 새 리마인더 폼(빠른 선택: 1시간 뒤, 오늘 밤 9시, 마감 하루 전 / 연결 도메인 + 대상 선택) |
| `/settings` | 설정 | 프로필 폼 · 화면(테마 다크/라이트/시스템, 언어, 강조색 5개 + 미리보기) · 알림 스위치 · API 연결(서버 주소, X-API-KEY 가림/보기, 연결 테스트 → 응답 시간 표시) · 주간 통계 다시 계산(기준 주 입력 + 통계 5종 각각 실행 버튼 + 결과 `#jobExecutionId status` 태그) |

- 라이트 테마는 토큰만 바꿔 동작하게 구조를 잡되, 1차 완성 기준은 다크 테마다
- 목표값(1,500kcal, 단백질 160g 이상, 탄수 140g 이하, 지방 50g 이하, 나트륨 2,000mg 대)은 서버에 없으니 `src/config/goals.ts`에 상수로 두고 설정에서 수정 가능하게(`localStorage`)

---

## 6. 진행 방식

1. 프로젝트 생성 → 토큰·글꼴·레이아웃(사이드바, 헤더, 모바일 탭) → API 클라이언트와 타입 → 공통 컴포넌트 순으로 만든다
2. 화면은 **설정 → 홈 → 건강 → 학업 → 공부 → 생활 → 일정 → 메모 → 허브 → 리마인더** 순서로 하나씩 완성한다 (설정이 먼저여야 API 키로 실제 서버에 붙여볼 수 있다)
3. 각 화면마다 로딩 / 빈 목록 / 오류 상태를 모두 구현한다
4. 서버 없이도 화면을 확인할 수 있게 MSW(Mock Service Worker)로 위 명세와 같은 모양의 목 데이터를 제공하고, `VITE_USE_MOCK=true`일 때만 켠다
5. 핵심 로직(날짜 파싱, 기간 필터, 히트맵 합산, D-day 계산, 봉투 해제·오류 매핑)은 Vitest 단위 테스트를 쓴다
6. 명세와 다르게 해야 할 부분이 생기면 임의로 바꾸지 말고 README의 "결정 사항"에 이유와 함께 적는다
