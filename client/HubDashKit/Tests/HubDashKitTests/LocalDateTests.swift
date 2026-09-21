import Foundation
import Testing
@testable import HubDashKit

@Suite("LocalDate / LocalDateTime")
struct LocalDateTests {
    @Test func parsesValidCalendarDate() {
        let date = LocalDate("2026-09-19")
        #expect(date == LocalDate(year: 2026, month: 9, day: 19))
        #expect(date?.description == "2026-09-19")
    }

    @Test(arguments: ["2026-02-30", "2026-13-01", "2026-1-5", "26-09-19", "2026/09/19", "2026-09-19T00:00:00", "", "abcd-ef-gh"])
    func rejectsInvalidDates(_ text: String) {
        #expect(LocalDate(text) == nil)
    }

    @Test func leapDayIsValidOnlyInLeapYears() {
        #expect(LocalDate("2028-02-29") != nil)
        #expect(LocalDate("2026-02-29") == nil)
    }

    @Test func codableRoundTripsAsPlainString() throws {
        let data = try JSONEncoder().encode(LocalDate(year: 2026, month: 9, day: 5))
        #expect(String(decoding: data, as: UTF8.self) == "\"2026-09-05\"")
        #expect(try JSONDecoder().decode(LocalDate.self, from: data) == LocalDate(year: 2026, month: 9, day: 5))
    }

    @Test func decodingAnInvalidDateThrows() {
        #expect(throws: DecodingError.self) { try JSONDecoder().decode(LocalDate.self, from: Data("\"2026-02-30\"".utf8)) }
    }

    @Test func isComparableByCalendarOrder() {
        #expect(LocalDate(year: 2026, month: 9, day: 19) < LocalDate(year: 2026, month: 10, day: 1))
        #expect(LocalDate(year: 2025, month: 12, day: 31) < LocalDate(year: 2026, month: 1, day: 1))
    }

    @Test func sameInstantIsDifferentCalendarDateInDifferentTimeZones() {
        // 2026-09-19 23:30 UTC 는 서울에서는 이미 9/20이다 — 달력 날짜를 Date로 다루면 하루가 밀리는 이유.
        let instant = LocalDateTimeCodec.parse("2026-09-19T23:30:00", in: utc)!
        #expect(LocalDate(instant, in: utc) == LocalDate(year: 2026, month: 9, day: 19))
        #expect(LocalDate(instant, in: seoul) == LocalDate(year: 2026, month: 9, day: 20))
    }

    // Jackson은 LocalDateTime의 소수 초를 1~9자리로 가변 출력한다(끝의 0을 생략) — 실제 서버 응답에서 확인.
    @Test(arguments: [
        ("2026-09-19T12:00:00", 0.0),
        ("2026-09-19T12:00:00.1", 0.1),
        ("2026-09-19T12:00:00.85", 0.85),
        ("2026-09-19T12:00:00.163843", 0.163843),
        ("2026-09-19T12:00:00.123456789", 0.123456789),
    ])
    func parsesVariableFractionalSeconds(_ text: String, _ fraction: Double) throws {
        let parsed = try #require(LocalDateTimeCodec.parse(text, in: utc))
        let base = try #require(LocalDateTimeCodec.parse("2026-09-19T12:00:00", in: utc))
        #expect(abs(parsed.timeIntervalSince(base) - fraction) < 1e-6)
    }

    @Test(arguments: ["", "2026-09-19", "2026-09-19 12:00:00", "2026-09-19T12:00", "2026-09-19T12:00:00.", "2026-09-19T12:00:00.abc",
                      "2026-09-19T12:00:00.1234567890", "2026-09-19T25:00:00", "2026-09-19T12:00:00Z"])
    func rejectsMalformedDateTimes(_ text: String) {
        #expect(LocalDateTimeCodec.parse(text, in: utc) == nil)
    }

    @Test func timeZoneShiftsTheInstantButKeepsTheWallClock() throws {
        let inSeoul = try #require(LocalDateTimeCodec.parse("2026-09-19T12:00:00", in: seoul))
        let inUTC = try #require(LocalDateTimeCodec.parse("2026-09-19T12:00:00", in: utc))
        #expect(inUTC.timeIntervalSince(inSeoul) == 9 * 3600)
        #expect(LocalDateTimeCodec.format(inSeoul, in: seoul) == "2026-09-19T12:00:00")
    }

    @Test func formatDropsTimeZoneAndFractionForTheServer() throws {
        let date = try #require(LocalDateTimeCodec.parse("2026-09-19T12:34:56.789", in: seoul))
        #expect(LocalDateTimeCodec.format(date, in: seoul) == "2026-09-19T12:34:56")
    }
}
