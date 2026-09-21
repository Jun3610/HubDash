import Foundation

public enum APIError: Error, Equatable, Sendable {
    /// 401 — API 키가 없거나 틀림
    case unauthorized
    /// 서버가 `{success:false, errorCode, message}` 봉투로 돌려준 오류(404 NOT_FOUND, 400 INVALID_REQUEST 등)
    case server(errorCode: String, message: String?, status: Int)
    /// 응답이 JSON 봉투가 아님(프록시/게이트웨이 오류 페이지 등)
    case invalidResponse(status: Int)
    /// 응답은 봉투였지만 기대한 모델로 해석하지 못함(서버와 클라이언트 모델이 어긋남)
    case decoding(String)
    /// 네트워크 자체가 실패함(연결 거부, 타임아웃 등)
    case transport(String)
}

extension APIError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .unauthorized: return "API 키가 올바르지 않습니다."
        case let .server(code, message, status): return "서버 오류 \(status) \(code): \(message ?? "")"
        case let .invalidResponse(status): return "서버 응답을 해석할 수 없습니다. (HTTP \(status))"
        case let .decoding(detail): return "응답 형식이 예상과 다릅니다: \(detail)"
        case let .transport(detail): return "네트워크 오류: \(detail)"
        }
    }
}
