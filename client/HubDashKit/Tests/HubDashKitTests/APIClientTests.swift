import Foundation
import Testing
@testable import HubDashKit

@Suite("APIClient")
struct APIClientTests {
    private func body(of request: URLRequest?) throws -> [String: Any] {
        let data = try #require(request?.httpBody)
        return try #require(try JSONSerialization.jsonObject(with: data) as? [String: Any])
    }

    // MARK: 요청 형태

    @Test func everyRequestCarriesTheApiKeyAndAcceptsJson() async throws {
        let transport = StubTransport(body: Sample.envelope(Sample.mealRecord))
        _ = try await makeClient(transport).meal(id: 1)
        let request = try #require(transport.lastRequest)
        #expect(request.value(forHTTPHeaderField: "X-API-KEY") == "test-key")
        #expect(request.value(forHTTPHeaderField: "Accept") == "application/json")
        #expect(request.httpMethod == "GET")
        #expect(request.url?.absoluteString == "http://hubdash.test/api/health/meal-records/1")
    }

    @Test func dailySummaryPassesTheDateAsAPlainCalendarDateQuery() async throws {
        let transport = StubTransport(body: Sample.envelope(Sample.dailySummary))
        _ = try await makeClient(transport).dailySummary(date: LocalDate(year: 2026, month: 9, day: 5))
        #expect(transport.lastRequest?.url?.absoluteString ==
                "http://hubdash.test/api/health/meal-records/daily-summary?date=2026-09-05")
    }

    @Test func listSendsPagingParameters() async throws {
        let transport = StubTransport(body: Sample.envelope(
            "{\"content\":[\(Sample.mealRecord)],\"page\":1,\"size\":5,\"totalElements\":6,\"totalPages\":2}"))
        let page = try await makeClient(transport).meals(page: 1, size: 5)
        #expect(transport.lastRequest?.url?.query == "page=1&size=5")
        #expect(page.content.count == 1)
        #expect(page.totalPages == 2)
    }

    @Test func createMealSendsLocalDateTimeWithoutZoneAndOmitsNilNotes() async throws {
        let transport = StubTransport(body: Sample.envelope(Sample.mealRecord))
        let consumedAt = try #require(LocalDateTimeCodec.parse("2026-09-19T12:00:00", in: seoul))
        _ = try await makeClient(transport).createMeal(MealRecordRequest(consumedAt: consumedAt, mealType: .lunch))
        let request = try #require(transport.lastRequest)
        #expect(request.httpMethod == "POST")
        #expect(request.value(forHTTPHeaderField: "Content-Type") == "application/json")
        let json = try body(of: request)
        #expect(json["consumedAt"] as? String == "2026-09-19T12:00:00")
        #expect(json["mealType"] as? String == "LUNCH")
        #expect(json["notes"] == nil) // nil은 null이 아니라 생략
    }

    @Test func addItemOmitsUnknownMacrosSoTheServerTreatsThemAsUnknown() async throws {
        let transport = StubTransport(body: Sample.envelope("""
        {"id":2,"mealRecordId":1,"name":"김치","calories":15,"carbsG":null,"proteinG":null,"fatG":null,"sodiumMg":null,
         "createdAt":"2026-09-19T22:40:01","updatedAt":"2026-09-19T22:40:01"}
        """))
        let item = try await makeClient(transport).addItem(MealItemRequest(mealRecordId: 1, name: "김치", calories: 15))
        let json = try body(of: transport.lastRequest)
        #expect(Set(json.keys) == ["mealRecordId", "name", "calories"])
        #expect(item.carbsG == nil)
    }

    @Test func updateItemUsesPutAndCanMoveToAnotherMeal() async throws {
        let transport = StubTransport(body: Sample.envelope("""
        {"id":7,"mealRecordId":2,"name":"밥","calories":300,"carbsG":66.0,"proteinG":5.0,"fatG":1.0,"sodiumMg":null,
         "createdAt":"2026-09-19T22:40:01","updatedAt":"2026-09-19T22:40:01"}
        """))
        let moved = try await makeClient(transport).updateItem(id: 7, MealItemRequest(mealRecordId: 2, name: "밥", calories: 300))
        #expect(transport.lastRequest?.httpMethod == "PUT")
        #expect(transport.lastRequest?.url?.path == "/api/health/meal-items/7")
        #expect(moved.mealRecordId == 2)
    }

    // MARK: 응답 해석

    @Test func decodesARealServerMealRecordIncludingNullsAndVariableFractions() async throws {
        let meal = try await makeClient(StubTransport(body: Sample.envelope(Sample.mealRecord))).meal(id: 1)
        #expect(meal.mealType == .lunch)
        #expect(meal.notes == nil)
        #expect(meal.items.map(\.name) == ["신라면", "김치"])
        #expect(meal.items[1].carbsG == nil)
        #expect(meal.totals == MealTotals(calories: 535, carbsG: 83.0, proteinG: 11.0, fatG: 16.0, sodiumMg: 1970.0))
        #expect(LocalDateTimeCodec.format(meal.consumedAt, in: seoul) == "2026-09-19T12:00:00")
    }

    @Test func decodesADailySummaryWithAllFourMealTypes() async throws {
        let summary = try await makeClient(StubTransport(body: Sample.envelope(Sample.dailySummary)))
            .dailySummary(date: LocalDate(year: 2026, month: 9, day: 19))
        #expect(summary.date == LocalDate(year: 2026, month: 9, day: 19))
        #expect(summary.totals.calories == 1210)
        #expect(summary.meals.map(\.mealType) == [.breakfast, .lunch, .dinner, .snack])
        #expect(summary.meals[2].itemCount == 0)
        #expect(summary.meals[3].totals.proteinG == 7.3)
    }

    @Test func deleteAcceptsNoContent() async throws {
        let transport = StubTransport(status: 204, body: "")
        try await makeClient(transport).deleteItem(id: 3)
        #expect(transport.lastRequest?.httpMethod == "DELETE")
        #expect(transport.lastRequest?.url?.path == "/api/health/meal-items/3")
    }

    // MARK: 오류 매핑

    @Test func http401IsUnauthorized() async {
        let transport = StubTransport(status: 401, body: "{\"success\":false,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"invalid api key\"}")
        await #expect(throws: APIError.unauthorized) { _ = try await makeClient(transport).meal(id: 1) }
    }

    @Test func serverErrorEnvelopeKeepsCodeMessageAndStatus() async {
        let transport = StubTransport(status: 404, body: "{\"data\":null,\"errorCode\":\"NOT_FOUND\",\"message\":\"meal record 9 not found\",\"success\":false}")
        await #expect(throws: APIError.server(errorCode: "NOT_FOUND", message: "meal record 9 not found", status: 404)) {
            _ = try await makeClient(transport).meal(id: 9)
        }
    }

    @Test func validationErrorIsReportedAsServerError() async {
        let transport = StubTransport(status: 400, body: "{\"data\":null,\"errorCode\":\"INVALID_REQUEST\",\"message\":\"0 이상이어야 합니다\",\"success\":false}")
        await #expect(throws: APIError.server(errorCode: "INVALID_REQUEST", message: "0 이상이어야 합니다", status: 400)) {
            _ = try await makeClient(transport).addItem(MealItemRequest(mealRecordId: 1, name: "x", calories: -1))
        }
    }

    @Test func htmlErrorPageFromAProxyIsAnInvalidResponse() async {
        let transport = StubTransport(status: 502, body: "<html><body>Bad Gateway</body></html>")
        await #expect(throws: APIError.invalidResponse(status: 502)) { _ = try await makeClient(transport).meal(id: 1) }
    }

    @Test func modelMismatchIsADecodingErrorThatNamesTheField() async {
        // 서버가 필드 이름을 바꾸면(mealType → type) 조용히 넘어가지 않고 어디가 어긋났는지 알려야 한다.
        let broken = Sample.mealRecord.replacingOccurrences(of: "\"mealType\"", with: "\"type\"")
        let transport = StubTransport(body: Sample.envelope(broken))
        do {
            _ = try await makeClient(transport).meal(id: 1)
            Issue.record("디코딩 오류가 나야 한다")
        } catch let APIError.decoding(detail) {
            #expect(detail.contains("mealType"))
        } catch {
            Issue.record("예상 밖 오류: \(error)")
        }
    }

    @Test func unknownMealTypeIsADecodingError() async {
        let broken = Sample.mealRecord.replacingOccurrences(of: "\"LUNCH\"", with: "\"BRUNCH\"")
        let transport = StubTransport(body: Sample.envelope(broken))
        await #expect(throws: APIError.self) { _ = try await makeClient(transport).meal(id: 1) }
    }

    @Test func successWithNullDataIsAnErrorNotACrash() async {
        let transport = StubTransport(body: "{\"data\":null,\"errorCode\":null,\"message\":null,\"success\":true}")
        await #expect(throws: APIError.self) { _ = try await makeClient(transport).meal(id: 1) }
    }

    @Test func networkFailureIsATransportError() async {
        struct Down: Error {}
        let transport = StubTransport { _ in throw Down() }
        do {
            _ = try await makeClient(transport).meal(id: 1)
            Issue.record("전송 오류가 나야 한다")
        } catch let APIError.transport(detail) {
            #expect(detail.contains("Down"))
        } catch {
            Issue.record("예상 밖 오류: \(error)")
        }
    }
}
