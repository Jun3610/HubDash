package com.junyoung.dashboard.domain.sync.source;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// iCloud가 CalDAV로 주는 iCalendar 모양(VTIMEZONE 포함)으로 변환·반복 펼치기를 확인한다 (이슈 #229)
class ICalendarEventsTest {

    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-31T00:00:00Z");

    private static final String SEOUL = """
            BEGIN:VTIMEZONE
            TZID:Asia/Seoul
            BEGIN:STANDARD
            DTSTART:19700101T000000
            TZOFFSETFROM:+0900
            TZOFFSETTO:+0900
            TZNAME:KST
            END:STANDARD
            END:VTIMEZONE
            """;

    private static String cal(String body) {
        return ("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Inc.//iCloud//EN\n" + SEOUL + body + "END:VCALENDAR\n")
                .replace("\n", "\r\n");
    }

    @Test
    void timedEventWithSeoulZone() {
        List<ExternalEvent> events = ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:AAA-111
                DTSTART;TZID=Asia/Seoul:20260915T143000
                DTEND;TZID=Asia/Seoul:20260915T160000
                SUMMARY:프젝 회의
                LOCATION:도서관 3층
                END:VEVENT
                """), FROM, TO);

        assertThat(events).containsExactly(new ExternalEvent("AAA-111", "프젝 회의",
                LocalDateTime.of(2026, 9, 15, 14, 30), LocalDateTime.of(2026, 9, 15, 16, 0), "도서관 3층", false));
    }

    @Test
    void utcTimeBecomesKstAndMissingEndStaysEmpty() {
        List<ExternalEvent> events = ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:BBB
                DTSTART:20260924T210000Z
                SUMMARY:OP6 출근
                END:VEVENT
                """), FROM, TO);

        assertThat(events).containsExactly(new ExternalEvent("BBB", "OP6 출근",
                LocalDateTime.of(2026, 9, 25, 6, 0), null, null, false));
    }

    @Test
    void allDaySingleAndMultiDay() {
        List<ExternalEvent> events = ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:ONE
                DTSTART;VALUE=DATE:20260924
                DTEND;VALUE=DATE:20260925
                SUMMARY:추석 연휴
                END:VEVENT
                BEGIN:VEVENT
                UID:MANY
                DTSTART;VALUE=DATE:20261020
                DTEND;VALUE=DATE:20261027
                SUMMARY:중간고사
                END:VEVENT
                """), FROM, TO);

        assertThat(events).containsExactlyInAnyOrder(
                new ExternalEvent("ONE", "추석 연휴", LocalDateTime.of(2026, 9, 24, 0, 0), null, null, true),
                new ExternalEvent("MANY", "중간고사", LocalDateTime.of(2026, 10, 20, 0, 0),
                        LocalDateTime.of(2026, 10, 27, 0, 0), null, true));
    }

    @Test
    void weeklyRecurrenceIsExpandedWithExdateAndOverride() {
        List<ExternalEvent> events = ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:GYM
                DTSTART;TZID=Asia/Seoul:20260901T190000
                DTEND;TZID=Asia/Seoul:20260901T200000
                RRULE:FREQ=WEEKLY;COUNT=4
                EXDATE;TZID=Asia/Seoul:20260908T190000
                SUMMARY:헬스
                END:VEVENT
                BEGIN:VEVENT
                UID:GYM
                RECURRENCE-ID;TZID=Asia/Seoul:20260915T190000
                DTSTART;TZID=Asia/Seoul:20260915T210000
                DTEND;TZID=Asia/Seoul:20260915T220000
                SUMMARY:헬스 (늦게)
                END:VEVENT
                """), FROM, TO);

        assertThat(events).containsExactlyInAnyOrder(
                new ExternalEvent("GYM@20260901T100000Z", "헬스",
                        LocalDateTime.of(2026, 9, 1, 19, 0), LocalDateTime.of(2026, 9, 1, 20, 0), null, false),
                new ExternalEvent("GYM@20260915T100000Z", "헬스 (늦게)",
                        LocalDateTime.of(2026, 9, 15, 21, 0), LocalDateTime.of(2026, 9, 15, 22, 0), null, false),
                new ExternalEvent("GYM@20260922T100000Z", "헬스",
                        LocalDateTime.of(2026, 9, 22, 19, 0), LocalDateTime.of(2026, 9, 22, 20, 0), null, false));
    }

    @Test
    void yearlyAllDayRecurrenceOnlyInsideWindow() {
        List<ExternalEvent> events = ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:BDAY
                DTSTART;VALUE=DATE:20201020
                DTEND;VALUE=DATE:20201021
                RRULE:FREQ=YEARLY
                SUMMARY:생일
                END:VEVENT
                """), FROM, TO);

        assertThat(events).containsExactly(
                new ExternalEvent("BDAY@20261020", "생일", LocalDateTime.of(2026, 10, 20, 0, 0), null, null, true));
    }

    @Test
    void cancelledAndBrokenAreSkipped() {
        assertThat(ICalendarEvents.parse(cal("""
                BEGIN:VEVENT
                UID:X
                DTSTART:20260924T010000Z
                STATUS:CANCELLED
                SUMMARY:취소됨
                END:VEVENT
                """), FROM, TO)).isEmpty();
        assertThat(ICalendarEvents.parse("이건 iCalendar가 아님", FROM, TO)).isEmpty();
    }
}
