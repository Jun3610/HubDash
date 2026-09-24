# HubDash 화면 디자인 원본

화면 하나 = 파일 하나. 브라우저에서 바로 열리는 파일이 아니라 **디자인 명세**다.
레이아웃, 색, 간격, 글자 크기가 전부 인라인 `style`에 정확한 값으로 적혀 있으니 그대로 옮겨서 구현한다.
눈으로 볼 때는 같이 전달한 PDF를 참고한다.

## 파일 읽는 법
- `<x-dc> … </x-dc>` 안의 마크업이 화면이다. `<helmet>`은 폰트와 body 기본값
- `{{이름}}` 은 파일 맨 아래 `renderVals()`가 돌려주는 값으로 채워진다. **목 데이터와 조건부 스타일이 거기 있다**
- `<sc-for list="{{items}}" as="x">` = 반복(`items.map`), `<sc-if value="{{cond}}">` = 조건부 렌더링
- `<dc-import name="Sidebar" active="health">` = `Sidebar.dc.html` 컴포넌트를 해당 메뉴가 선택된 상태로 넣는다는 뜻
- `href="Pknu.dc.html"` 같은 링크 = 해당 화면 라우트로 이동
- `hint-*`, `data-props`, `support.js`, `DCLogic` 는 디자인 도구 전용이라 무시한다

## 화면 ↔ 라우트
| 파일 | 라우트 | 크기 |
|---|---|---|
| Main.dc.html | `/` 홈 | 1440×1180 |
| Health.dc.html | `/health` | 1440×1024 |
| Pknu.dc.html | `/pknu` | 1440×1024 |
| Study.dc.html | `/study` | 1440×1024 |
| Life.dc.html | `/life` | 1440×1024 |
| Schedule.dc.html | `/schedule` | 1440×1024 |
| Memo.dc.html | `/memo` | 1440×1024 |
| Hub.dc.html | `/hub` | 1440×1024 |
| Reminder.dc.html | `/reminders` | 1440×1024 |
| Settings.dc.html | `/settings` | 1440×1024 |
| Mobile.dc.html | 모바일 홈 (<768px) | 390×844 |
| Sidebar.dc.html | 공통 사이드바 컴포넌트 | 232×1024 |
| Guide.dc.html | 디자인 토큰 · 컴포넌트 · 상태 화면 가이드 | 1440×1180 |

## 주의
- 고정 크기(1440px 등)는 디자인 캔버스 크기일 뿐이다. 실제 구현은 반응형으로 한다
- hex 값은 컴포넌트에 직접 쓰지 말고 `Guide.dc.html`과 프롬프트의 CSS 변수(토큰)로 옮긴다
- 화면 속 숫자와 문구(과목명, 칼로리 등)는 목 데이터다. 실제 값은 API에서 가져온다
