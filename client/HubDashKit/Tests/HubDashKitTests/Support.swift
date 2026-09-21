import Foundation
@testable import HubDashKit

/// 요청을 기록하고 미리 정한 응답을 돌려주는 스텁 전송 계층.
final class StubTransport: HTTPTransport, @unchecked Sendable {
    private let lock = NSLock()
    private var _requests: [URLRequest] = []
    private let responder: @Sendable (URLRequest) throws -> (Int, Data)

    init(_ responder: @escaping @Sendable (URLRequest) throws -> (Int, Data)) {
        self.responder = responder
    }

    convenience init(status: Int = 200, body: String) {
        self.init { _ in (status, Data(body.utf8)) }
    }

    var requests: [URLRequest] { lock.lock(); defer { lock.unlock() }; return _requests }
    var lastRequest: URLRequest? { requests.last }

    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        lock.lock(); _requests.append(request); lock.unlock()
        let (status, data) = try responder(request)
        let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!
        return (data, response)
    }
}

let seoul = TimeZone(identifier: "Asia/Seoul")!
let utc = TimeZone(identifier: "UTC")!

func makeClient(_ transport: HTTPTransport, timeZone: TimeZone = seoul) -> APIClient {
    APIClient(baseURL: URL(string: "http://hubdash.test")!, apiKey: "test-key", transport: transport, timeZone: timeZone)
}

/// 실제 서버(2026-09-19 검증)가 돌려준 응답 형태 — 소수 초 자릿수가 가변이고 null 필드가 섞여 있다.
enum Sample {
    static let mealRecord = """
    {"id":1,"consumedAt":"2026-09-19T12:00:00","mealType":"LUNCH","notes":null,
     "items":[
       {"id":1,"mealRecordId":1,"name":"신라면","calories":520,"carbsG":83.0,"proteinG":11.0,"fatG":16.0,"sodiumMg":1970.0,
        "createdAt":"2026-09-19T22:39:59.163843","updatedAt":"2026-09-19T22:39:59.163843"},
       {"id":2,"mealRecordId":1,"name":"김치","calories":15,"carbsG":null,"proteinG":null,"fatG":null,"sodiumMg":null,
        "createdAt":"2026-09-19T22:40:01.1","updatedAt":"2026-09-19T22:40:01"}],
     "totals":{"calories":535,"carbsG":83.0,"proteinG":11.0,"fatG":16.0,"sodiumMg":1970.0},
     "createdAt":"2026-09-19T22:39:58.85","updatedAt":"2026-09-19T22:40:01.123456789"}
    """

    static func envelope(_ data: String) -> String {
        "{\"data\":\(data),\"errorCode\":null,\"message\":null,\"success\":true}"
    }

    static let dailySummary = """
    {"date":"2026-09-19","totals":{"calories":1210,"carbsG":187.0,"proteinG":35.3,"fatG":33.4,"sodiumMg":2110.0},
     "meals":[
       {"mealType":"BREAKFAST","itemCount":2,"totals":{"calories":450,"carbsG":67.0,"proteinG":17.0,"fatG":11.0,"sodiumMg":140.0}},
       {"mealType":"LUNCH","itemCount":2,"totals":{"calories":535,"carbsG":83.0,"proteinG":11.0,"fatG":16.0,"sodiumMg":1970.0}},
       {"mealType":"DINNER","itemCount":0,"totals":{"calories":0,"carbsG":0.0,"proteinG":0.0,"fatG":0.0,"sodiumMg":0.0}},
       {"mealType":"SNACK","itemCount":2,"totals":{"calories":225,"carbsG":37.0,"proteinG":7.3,"fatG":6.4,"sodiumMg":0.0}}]}
    """
}
