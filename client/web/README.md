# HubDash 웹 클라이언트

HubDash 서버(`../../server`, Spring Boot REST API)에 붙는 개인용 대시보드 웹 앱입니다.
디자인 원본은 `../hubdash-design/`(화면별 `*.dc.html`, `PROMPT.md`)에 있고, 그 값을 그대로 옮겨 구현합니다.

## 실행

```bash
cd client/web
npm install

# 1) 실제 서버에 붙여서 (서버는 ../../server 에서 docker compose up -d)
npm run dev                      # http://localhost:5173 , /api → http://localhost:8080 프록시

# 2) 서버 없이 목 데이터로
VITE_USE_MOCK=true npm run dev   # MSW가 명세와 같은 모양으로 응답
```

| 명령 | 내용 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 타입 검사 + 프로덕션 빌드 (`dist/`) |
| `npm run typecheck` | `tsc -b` (strict) |
| `npm run lint` | oxlint |
| `npm test` | Vitest 단위 테스트 |
| `npm run format` | Prettier |

## 환경 변수

`.env.example`을 `.env.local`로 복사해서 씁니다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `VITE_USE_MOCK` | `false` | `true`면 MSW 목 서버로 동작 (서버 불필요) |
| `VITE_PROXY_TARGET` | `http://localhost:8080` | 개발 프록시가 `/api`를 넘길 서버 |

서버 주소와 `X-API-KEY`(로컬 기본값 `dev-local-key`)는 앱의 **설정 → API 연결**에서 바꾸고 `localStorage`에 저장합니다.

### CORS 안내

서버에는 CORS 설정이 없습니다. 그래서 기본 주소(`http://localhost:8080`)일 때 앱은 상대 경로 `/api/...`로 호출하고 Vite 프록시가 서버로 넘깁니다(같은 출처라 CORS 불필요).
설정에서 **다른 주소로 바꾸면 절대 경로로 직접 호출**하므로 서버에 CORS 허용이 필요합니다. 이때 브라우저는 사전 요청(`OPTIONS`)을 `X-API-KEY` 없이 보내는데, 서버의 `ApiKeyAuthFilter`가 모든 요청에 키를 요구하므로 `OPTIONS`도 통과시키도록 서버를 함께 고쳐야 합니다.
같은 PC가 아닌 곳에서 쓸 때는 주소를 바꾸기보다 `VITE_PROXY_TARGET`으로 프록시 대상을 바꾸는 편이 간단합니다.

## 폴더 구조

```
src/
  api/            client.ts(요청·봉투 해제·오류 매핑·연결 상태), types.ts(서버 DTO 타입),
                  resource.ts(CRUD 공통 + TanStack Query 훅), 도메인별 {pknu,study,life,health,
                  schedule,memo,hub,reminder,user,analytics}.ts
  components/
    layout/       AppLayout, Sidebar, PageHeader, 모바일 헤더·하단 탭, 전역 배너, ⌘K 검색, 빠른 기록
    ui/           Card, SectionHeader, Tabs, Segmented, Tag, Counter, DdayBadge, ProgressBar, KpiTile,
                  YearHeatmap, BarChart, HBarList, Sparkline, Button, IconButton, Field, Input, Select,
                  Textarea, Checkbox, Switch, Toast, EmptyState, Skeleton, QueryState, Modal, ConfirmDialog
  config/         connection.ts(서버 주소·키), goals.ts(식단 목표), prefs.ts(강조색·고정 링크·학기 다짐·테마)
  hooks/          여러 화면이 같이 쓰는 조회 묶음 (현재 학기+과목+과제, 허브 전체 링크, 기록 히트맵 합산)
  lib/            date.ts(LocalDate/LocalDateTime·주·D-day), heatmap.ts, format.ts, storage.ts,
                  select/*.ts(프론트 기간 필터 — 서버 필터가 생기면 여기만 바꾼다)
  mocks/          MSW 핸들러와 목 데이터 (VITE_USE_MOCK=true일 때만)
  pages/          라우트별 화면
  styles/         tokens.css(디자인 토큰, 강조색 프리셋 5개, 라이트 테마), global.css
```

## 서버 연동 규칙

- 모든 요청에 `X-API-KEY` 헤더, 응답은 `{ success, data, errorCode, message }` 봉투
- 오류 처리: 401 → 상단 "API 키를 확인해 주세요" 배너 / 서버 꺼짐 → "서버에 연결할 수 없어요 · 다시 시도" 배너 / `success:false` → `errorCode`·`message` 토스트
- 날짜는 문자열 그대로 다루고, 계산이 필요할 때만 `lib/date.ts`로 로컬 시각으로 읽습니다 (`new Date("2026-09-23")`은 UTC로 읽혀 쓰지 않음). `LocalDateTime` 소수 초는 0~9자리 모두 읽습니다
- 주 계산은 서버 주간 통계와 같이 **월요일 시작**

## 결정 사항

명세(`../hubdash-design/PROMPT.md`)와 다르게 하거나 명세에 없는 부분을 정한 것들입니다.

| 항목 | 결정 | 이유 |
|---|---|---|
| React 버전 | 18.3 고정 | 명세가 React 18. Vite 템플릿 기본값(19)에서 내림 |
| 린터 | oxlint | Vite 템플릿 기본. ESLint보다 빠르고 설정이 필요 없음 |
| 서버 검증 오류 위치 | 필드 아래가 아니라 **폼 하단**에 `errorCode` + `message` | 서버 `INVALID_REQUEST` 메시지가 필드 이름 없이 `"must not be blank, …"`처럼 합쳐져 와서 어느 필드인지 알 수 없음. 프론트 사전 검사 오류는 필드 아래에 표시 |
| 기록 히트맵의 "과제" | 완료된 과제의 `updatedAt` 날짜 | 서버에 완료 시각 필드가 없음 |
| 히트맵 단계 | 기간 내 최댓값 대비 4단계 | 도메인이 많아 하루 건수 편차가 커서 고정 구간보다 읽기 쉬움. 사이드바 주간 칸은 최근 4주 최댓값 기준 |
| 1년치 조회 크기 | 히트맵 원천 목록은 `size=2000` | 서버에 날짜 범위 필터가 없고, Spring 기본 최대 페이지 크기가 2000 |
| 테마 값 | 서버에 `DARK`/`LIGHT`/`SYSTEM` 대문자로 저장 | 서버 기본값이 `"LIGHT"`(대문자) |
| 개발 프록시 대상 | `VITE_PROXY_TARGET`으로 바꿀 수 있게 함 | 명세에 없음. 서버 꺼짐 상황을 재현하거나 다른 PC의 서버에 붙을 때 필요 |
| Vite 프록시의 서버 꺼짐 응답 | 본문 없는 5xx(실제로 502)는 네트워크 오류로 처리 | 프록시를 거치면 fetch 자체는 성공하고 502만 돌아옴 |
