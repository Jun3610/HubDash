# HubDash 클라이언트

> **현재 트랙은 웹입니다 (이슈 #93).** React 웹 클라이언트는 [`web/`](web/README.md)에 있습니다.
> 아래 iOS/macOS(`HubDashKit`) 내용은 웹으로 전환하기 전 기록으로 남겨 둡니다.

노션 마이그레이션의 두 번째 목표인 "Mac·iOS에서 같은 대시보드를 쓰는 클라이언트"입니다. 서버(`../server`)는 화면 없는 순수 REST API입니다.

## 결정 (이슈 #57, #84)

| 항목 | 결정 |
|---|---|
| 위치 | 이 레포의 `client/` |
| 스택 | SwiftUI(iOS + macOS). 핵심 로직은 Swift Package `HubDashKit`에 두고, 화면 앱은 그 위에 얹는다 |
| 서버 연동 | `X-API-KEY` 헤더 + `{data, errorCode, message, success}` 봉투 + 페이지 응답(`{content, page, size, totalElements, totalPages}`) |

핵심 로직을 패키지로 분리한 이유는 화면 없이 `swift build` / `swift test`만으로 API 층을 검증할 수 있고, iOS와 macOS 앱이 같은 코드를 공유하기 때문입니다.

## 현재 상태

- ✅ `HubDashKit`: API 클라이언트, 오류 매핑, `LocalDate`/`LocalDateTime` 코덱, **식단 도메인**(끼니, 음식 항목, 하루 합계)
- ⬜ 화면 앱(SwiftUI), 나머지 도메인 API — 후속 이슈. 화면 앱은 **Xcode가 필요**합니다(현재 개발 환경에는 Command Line Tools만 있어 iOS SDK/시뮬레이터/`xcodebuild`를 쓸 수 없음).

## HubDashKit 사용

```swift
let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, apiKey: "dev-local-key")

let meal = try await client.createMeal(MealRecordRequest(consumedAt: Date(), mealType: .lunch))
_ = try await client.addItem(MealItemRequest(mealRecordId: meal.id, name: "신라면", calories: 520,
                                             carbsG: 83, proteinG: 11, fatG: 16, sodiumMg: 1970))
let summary = try await client.dailySummary(date: LocalDate(Date()))   // 하루 총합 + 아침/점심/저녁/간식별 합계
```

오류는 `APIError`로 구분됩니다: `.unauthorized`(401), `.server(errorCode:message:status:)`(404 NOT_FOUND, 400 INVALID_REQUEST 등), `.invalidResponse`(프록시 오류 페이지 등), `.decoding`(서버와 모델이 어긋남 — 어느 필드인지 메시지에 포함), `.transport`(연결 실패).

### 날짜 다루기

서버의 `LocalDate`/`LocalDateTime`은 시간대 정보가 없습니다. 달력 날짜(`LocalDate`)는 `Date`가 아니라 별도 타입으로 다뤄, 시간대에 따라 하루가 밀리는 문제를 피합니다(UTC 23:30은 서울에서 이미 다음 날). 서버 `LocalDateTime`의 소수 초는 1~9자리로 가변이라(Jackson이 끝의 0을 생략) 자릿수에 상관없이 읽습니다.

## 테스트

```bash
cd client/HubDashKit
swift test                                   # 단위 테스트(스텁 전송 계층, 서버 불필요)
```

**계약 테스트**는 실제 서버에 붙어서 Swift 모델이 진짜 응답을 해석하는지 확인합니다. 서버를 띄우고 환경변수를 주면 실행됩니다(없으면 건너뜀).

```bash
docker compose up -d db kafka && (cd server && ./gradlew :api:bootRun)   # 레포 루트에서, 다른 터미널
cd client/HubDashKit
HUBDASH_BASE_URL=http://localhost:8080 HUBDASH_API_KEY=dev-local-key swift test
```

서버가 필드 이름이나 형식을 바꾸면 단위 테스트(스텁)는 그대로 통과하지만 계약 테스트가 실패합니다. 실패하는지 확인하려고 모델의 열거형 값을 일부러 틀리게 바꿔 봤고, 요청 방향(서버가 400으로 거부)과 응답 방향(`data.meals.Index 1.mealType` 디코딩 오류) 양쪽에서 잡히는 것을 확인했습니다.

> `Package.swift`에 `platforms`를 명시해야 합니다. 생략하면 SwiftPM이 macOS 10.13을 기본으로 잡아 swift-testing 매크로가 컴파일되지 않습니다.
