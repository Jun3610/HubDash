import Foundation
import Testing
@testable import HubDashKit

/// 실제 HubDash 서버에 붙어서 Swift 모델이 진짜 응답을 해석하는지 확인하는 계약 테스트.
/// 서버를 띄우고 환경변수를 주면 실행된다(없으면 건너뜀):
///   HUBDASH_BASE_URL=http://localhost:8080 HUBDASH_API_KEY=dev-local-key swift test
/// 서버 쪽 필드 이름/형식이 바뀌면 단위 테스트(스텁)는 그대로 통과하지만 여기서 실패한다.
private let env = ProcessInfo.processInfo.environment
private let liveServerConfigured = env["HUBDASH_BASE_URL"] != nil && env["HUBDASH_API_KEY"] != nil

@Suite("서버 계약 (실제 서버 필요)", .enabled(if: liveServerConfigured), .serialized)
struct ServerContractTests {
    private let client = APIClient(baseURL: URL(string: env["HUBDASH_BASE_URL"] ?? "http://unused")!,
                                   apiKey: env["HUBDASH_API_KEY"] ?? "", timeZone: seoul)

    private func at(_ text: String) -> Date { LocalDateTimeCodec.parse(text, in: seoul)! }

    /// 매 실행마다 다른 먼 과거 날짜를 쓴다. 고정 날짜를 쓰면 이전 실행이 중간에 실패해 남긴 데이터나 연속 실행에 취약하다.
    private func uniqueDay() -> LocalDate {
        LocalDate(year: Int.random(in: 1900...1999), month: Int.random(in: 1...12), day: Int.random(in: 1...28))
    }

    private func at(_ day: LocalDate, _ time: String) -> Date { at("\(day)T\(time)") }

    @Test func mealItemsRollUpIntoTotalsAndDailySummary() async throws {
        let day = uniqueDay()
        let breakfast = try await client.createMeal(MealRecordRequest(consumedAt: at(day, "08:00:00"), mealType: .breakfast))
        let lunch = try await client.createMeal(MealRecordRequest(consumedAt: at(day, "12:30:00"), mealType: .lunch, notes: "회사 근처"))
        #expect(breakfast.items.isEmpty)
        #expect(breakfast.totals == MealTotals(calories: 0, carbsG: 0, proteinG: 0, fatG: 0, sodiumMg: 0))
        #expect(lunch.notes == "회사 근처")

        _ = try await client.addItem(MealItemRequest(mealRecordId: breakfast.id, name: "밥", calories: 300, carbsG: 66, proteinG: 5, fatG: 1))
        _ = try await client.addItem(MealItemRequest(mealRecordId: breakfast.id, name: "계란", calories: 150, carbsG: 1, proteinG: 12, fatG: 10, sodiumMg: 140))
        let kimchi = try await client.addItem(MealItemRequest(mealRecordId: lunch.id, name: "김치", calories: 15)) // 탄단지 모름
        #expect(kimchi.carbsG == nil)
        #expect(kimchi.name == "김치")

        let reloaded = try await client.meal(id: breakfast.id)
        #expect(reloaded.items.map(\.name) == ["밥", "계란"])
        #expect(reloaded.totals == MealTotals(calories: 450, carbsG: 67, proteinG: 17, fatG: 11, sodiumMg: 140))

        let summary = try await client.dailySummary(date: day)
        #expect(summary.date == day)
        #expect(summary.totals.calories == 465)
        #expect(summary.meals.map(\.mealType) == [.breakfast, .lunch, .dinner, .snack])
        #expect(summary.meals[0].itemCount == 2)
        #expect(summary.meals[1].totals.calories == 15)
        #expect(summary.meals[2].itemCount == 0) // 기록 없는 끼니도 0으로 채워져 온다

        // 항목을 다른 끼니로 옮기면 합계가 따라간다
        _ = try await client.updateItem(id: kimchi.id, MealItemRequest(mealRecordId: breakfast.id, name: "김치", calories: 15))
        let moved = try await client.dailySummary(date: day)
        #expect(moved.meals[0].itemCount == 3)
        #expect(moved.meals[1].itemCount == 0)

        try await client.deleteItem(id: kimchi.id)
        #expect(try await client.dailySummary(date: day).totals.calories == 450)

        // 끼니를 지우면 딸린 항목도 함께 사라진다
        try await client.deleteMeal(id: breakfast.id)
        try await client.deleteMeal(id: lunch.id)
        #expect(try await client.dailySummary(date: day).totals.calories == 0)
    }

    @Test func emptyDayReturnsZeroTotalsAndAllFourMealTypes() async throws {
        let summary = try await client.dailySummary(date: uniqueDay())
        #expect(summary.totals == MealTotals(calories: 0, carbsG: 0, proteinG: 0, fatG: 0, sodiumMg: 0))
        #expect(summary.meals.count == 4)
    }

    @Test func listDecodesTheServerPageEnvelope() async throws {
        let meal = try await client.createMeal(MealRecordRequest(consumedAt: at(uniqueDay(), "19:00:00"), mealType: .dinner))
        defer { Task { try? await client.deleteMeal(id: meal.id) } }
        let page = try await client.meals(page: 0, size: 100)
        #expect(page.size == 100)
        #expect(page.content.contains { $0.id == meal.id })
        #expect(page.totalElements >= 1)
    }

    @Test func createdAtTimestampsFromTheServerAreDecodedDespiteVariableFractionDigits() async throws {
        let meal = try await client.createMeal(MealRecordRequest(consumedAt: at(uniqueDay(), "07:00:00"), mealType: .breakfast))
        defer { Task { try? await client.deleteMeal(id: meal.id) } }
        // 서버가 방금 만든 값이므로 클라이언트 시각과 크게 어긋나지 않아야 한다(시간대 해석이 맞는지의 확인).
        #expect(abs(meal.createdAt.timeIntervalSinceNow) < 300)
    }

    @Test func unknownMealIsANotFoundServerError() async {
        await #expect(throws: APIError.server(errorCode: "NOT_FOUND", message: "meal record 999999999 not found", status: 404)) {
            _ = try await client.meal(id: 999_999_999)
        }
    }

    @Test func addingAnItemToAMissingMealIsANotFound() async {
        do {
            _ = try await client.addItem(MealItemRequest(mealRecordId: 999_999_999, name: "밥", calories: 300))
            Issue.record("404가 나야 한다")
        } catch let APIError.server(code, _, status) {
            #expect(code == "NOT_FOUND")
            #expect(status == 404)
        } catch {
            Issue.record("예상 밖 오류: \(error)")
        }
    }

    @Test func serverSideValidationIsSurfacedAsInvalidRequest() async throws {
        let meal = try await client.createMeal(MealRecordRequest(consumedAt: at(uniqueDay(), "07:00:00"), mealType: .snack))
        defer { Task { try? await client.deleteMeal(id: meal.id) } }
        do {
            _ = try await client.addItem(MealItemRequest(mealRecordId: meal.id, name: "밥", calories: -1))
            Issue.record("400이 나야 한다")
        } catch let APIError.server(code, _, status) {
            #expect(code == "INVALID_REQUEST")
            #expect(status == 400)
        } catch {
            Issue.record("예상 밖 오류: \(error)")
        }
    }

    @Test func wrongApiKeyIsUnauthorized() async {
        let bad = APIClient(baseURL: URL(string: env["HUBDASH_BASE_URL"] ?? "http://unused")!, apiKey: "definitely-wrong", timeZone: seoul)
        await #expect(throws: APIError.unauthorized) { _ = try await bad.meal(id: 1) }
    }

    @Test func unreachableServerIsATransportError() async {
        let down = APIClient(baseURL: URL(string: "http://127.0.0.1:1")!, apiKey: "x", timeZone: seoul)
        do {
            _ = try await down.meal(id: 1)
            Issue.record("전송 오류가 나야 한다")
        } catch APIError.transport {
        } catch {
            Issue.record("예상 밖 오류: \(error)")
        }
    }
}
