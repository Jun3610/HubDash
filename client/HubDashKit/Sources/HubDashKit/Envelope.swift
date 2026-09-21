import Foundation

/// 서버의 공통 응답 봉투 `{data, errorCode, message, success}`.
struct Envelope<T: Decodable>: Decodable {
    let success: Bool
    let data: T?
    let errorCode: String?
    let message: String?
}

/// 서버의 페이지 응답 `{content, page, size, totalElements, totalPages}`.
public struct Page<T: Decodable & Sendable>: Decodable, Sendable {
    public let content: [T]
    public let page: Int
    public let size: Int
    public let totalElements: Int
    public let totalPages: Int
}
