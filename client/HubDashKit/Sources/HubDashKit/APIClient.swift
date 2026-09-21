import Foundation

/// 네트워크 전송 계층. 테스트에서 스텁으로 바꿔 끼울 수 있게 분리했다.
public protocol HTTPTransport: Sendable {
    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse)
}

public struct URLSessionTransport: HTTPTransport {
    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse(status: -1) }
        return (data, http)
    }
}

/// HubDash 서버 REST API 클라이언트. 모든 요청에 `X-API-KEY`를 붙이고 응답 봉투를 벗겨 준다.
public struct APIClient: Sendable {
    public let baseURL: URL
    let apiKey: String
    let transport: HTTPTransport
    /// 서버의 시간대 없는 날짜/시각(LocalDateTime)을 `Date`로 바꿀 때 쓰는 시간대.
    let timeZone: TimeZone

    public init(baseURL: URL, apiKey: String, transport: HTTPTransport = URLSessionTransport(),
                timeZone: TimeZone = .current) {
        self.baseURL = baseURL
        self.apiKey = apiKey
        self.transport = transport
        self.timeZone = timeZone
    }

    // MARK: 요청

    func get<T: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> T {
        try await send(method: "GET", path: path, query: query, body: Optional<Data>.none)
    }

    func post<Body: Encodable, T: Decodable>(_ path: String, body: Body) async throws -> T {
        try await send(method: "POST", path: path, query: [], body: try encode(body))
    }

    func put<Body: Encodable, T: Decodable>(_ path: String, body: Body) async throws -> T {
        try await send(method: "PUT", path: path, query: [], body: try encode(body))
    }

    /// 본문이 없는 성공 응답(204 No Content)을 기대하는 삭제.
    func delete(_ path: String) async throws {
        let (data, http) = try await execute(method: "DELETE", path: path, query: [], body: nil)
        if (200..<300).contains(http.statusCode) && data.isEmpty { return }
        _ = try unwrap(data, http) as EmptyData?
    }

    // MARK: 내부

    private struct EmptyData: Decodable {}

    private func send<T: Decodable>(method: String, path: String, query: [URLQueryItem], body: Data?) async throws -> T {
        let (data, http) = try await execute(method: method, path: path, query: query, body: body)
        guard let value: T = try unwrap(data, http) else {
            throw APIError.decoding("응답의 data가 비어 있습니다")
        }
        return value
    }

    private func execute(method: String, path: String, query: [URLQueryItem], body: Data?) async throws -> (Data, HTTPURLResponse) {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw APIError.transport("잘못된 URL: \(path)") }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue(apiKey, forHTTPHeaderField: "X-API-KEY")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        do {
            return try await transport.send(request)
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.transport(String(describing: error))
        }
    }

    /// 봉투를 벗기고 오류를 `APIError`로 바꾼다. 성공인데 data가 null이면 nil을 돌려준다.
    private func unwrap<T: Decodable>(_ data: Data, _ http: HTTPURLResponse) throws -> T? {
        if http.statusCode == 401 { throw APIError.unauthorized }
        let envelope: Envelope<T>
        do {
            envelope = try decoder.decode(Envelope<T>.self, from: data)
        } catch let error as DecodingError where Self.isEnvelopeShape(data) {
            // 봉투 자체는 맞는데 data를 우리 모델로 읽지 못한 경우 — 서버와 클라이언트 모델이 어긋난 것.
            throw APIError.decoding(Self.describe(error))
        } catch {
            throw APIError.invalidResponse(status: http.statusCode)
        }
        guard envelope.success else {
            throw APIError.server(errorCode: envelope.errorCode ?? "UNKNOWN", message: envelope.message, status: http.statusCode)
        }
        return envelope.data
    }

    private static func isEnvelopeShape(_ data: Data) -> Bool {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return false }
        return object["success"] != nil
    }

    private static func describe(_ error: DecodingError) -> String {
        func path(_ context: DecodingError.Context) -> String {
            context.codingPath.map(\.stringValue).joined(separator: ".")
        }
        switch error {
        case let .keyNotFound(key, context): return "필드 없음 '\(key.stringValue)' (\(path(context)))"
        case let .typeMismatch(_, context), let .valueNotFound(_, context), let .dataCorrupted(context):
            return "\(context.debugDescription) (\(path(context)))"
        @unknown default: return String(describing: error)
        }
    }

    // MARK: 인코딩/디코딩

    private var decoder: JSONDecoder {
        let decoder = JSONDecoder()
        let zone = timeZone
        decoder.dateDecodingStrategy = .custom { decoder in
            let value = try decoder.singleValueContainer().decode(String.self)
            guard let date = LocalDateTimeCodec.parse(value, in: zone) else {
                throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath,
                                                        debugDescription: "잘못된 날짜/시각 형식: \(value)"))
            }
            return date
        }
        return decoder
    }

    private func encode<Body: Encodable>(_ body: Body) throws -> Data {
        let encoder = JSONEncoder()
        let zone = timeZone
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(LocalDateTimeCodec.format(date, in: zone))
        }
        return try encoder.encode(body)
    }
}
