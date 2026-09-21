import Foundation

/// 서버의 `LocalDate`("yyyy-MM-dd")에 대응하는 시간대 없는 달력 날짜.
/// `Date`로 다루면 시간대에 따라 날짜가 하루 밀릴 수 있어, 달력 날짜는 별도 타입으로 둔다.
public struct LocalDate: Codable, Hashable, Comparable, CustomStringConvertible, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    /// `date`를 `timeZone`의 달력 날짜로 바꾼다.
    public init(_ date: Date, in timeZone: TimeZone = .current) {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(year: parts.year!, month: parts.month!, day: parts.day!)
    }

    /// "yyyy-MM-dd". 실제로 존재하지 않는 날짜(2026-02-30 등)는 nil.
    public init?(_ string: String) {
        let parts = string.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3, parts[0].count == 4, parts[1].count == 2, parts[2].count == 2,
              let year = Int(parts[0]), let month = Int(parts[1]), let day = Int(parts[2]) else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let components = DateComponents(year: year, month: month, day: day)
        // 유효하지 않은 날짜는 Calendar가 다른 날짜로 "보정"하므로, 되돌려 읽어 같은지 확인한다.
        guard let date = calendar.date(from: components),
              calendar.dateComponents([.year, .month, .day], from: date) == components else { return nil }
        self.init(year: year, month: month, day: day)
    }

    public var description: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    public static func < (lhs: LocalDate, rhs: LocalDate) -> Bool {
        (lhs.year, lhs.month, lhs.day) < (rhs.year, rhs.month, rhs.day)
    }

    public init(from decoder: Decoder) throws {
        let value = try decoder.singleValueContainer().decode(String.self)
        guard let parsed = LocalDate(value) else {
            throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath,
                                                    debugDescription: "잘못된 날짜 형식: \(value)"))
        }
        self = parsed
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(description)
    }
}

/// 서버의 `LocalDateTime`은 시간대 정보가 없다("2026-09-19T12:00:00", 소수 초는 1~9자리로 가변).
/// 앱에서는 `Date`로 다루되, 변환에 쓸 시간대를 명시적으로 받는다.
enum LocalDateTimeCodec {
    static func parse(_ string: String, in timeZone: TimeZone) -> Date? {
        // "2026-09-19T16:57:41.85"처럼 소수 초 자릿수가 일정하지 않아 초 단위까지와 소수부를 따로 처리한다.
        let halves = string.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        guard let base = baseFormatter(timeZone).date(from: String(halves[0])) else { return nil }
        guard halves.count == 2 else { return base }
        let digits = halves[1]
        guard (1...9).contains(digits.count), digits.allSatisfy(\.isNumber), let fraction = Double("0." + digits) else { return nil }
        return base.addingTimeInterval(fraction)
    }

    static func format(_ date: Date, in timeZone: TimeZone) -> String {
        baseFormatter(timeZone).string(from: date)
    }

    private static func baseFormatter(_ timeZone: TimeZone) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return formatter
    }
}
