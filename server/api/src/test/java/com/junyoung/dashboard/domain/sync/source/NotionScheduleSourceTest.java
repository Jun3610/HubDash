package com.junyoung.dashboard.domain.sync.source;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 노션 API 응답 모양(속성 이름은 실제 Schedule DB 그대로) → 일정 변환 (이슈 #228)
class NotionScheduleSourceTest {

    static Map<String, Object> row(String id, String title, Map<String, Object> date, String location) {
        Map<String, Object> props = new HashMap<>();
        props.put("Event", Map.of("type", "title", "title", List.of(Map.of("plain_text", title))));
        if (date != null) {
            props.put("Time Slot", Map.of("type", "date", "date", date));
        }
        props.put("Location", Map.of("type", "rich_text", "rich_text",
                location == null ? List.of() : List.of(Map.of("plain_text", location))));
        props.put("Status", Map.of("type", "select", "select", Map.of("name", "Completed")));
        props.put("Date", Map.of("type", "formula", "formula", Map.of("type", "string", "string", "x")));
        return Map.of("object", "page", "id", id, "properties", props);
    }

    static Map<String, Object> date(String start, String end) {
        Map<String, Object> d = new HashMap<>();
        d.put("start", start);
        d.put("end", end);
        d.put("time_zone", null);
        return d;
    }

    static ExternalEvent convert(Map<String, Object> row) {
        return NotionScheduleSource.convert(row);
    }

    @Test
    void timedUtcBecomesKstAndMissingEndStaysEmpty() {
        ExternalEvent e = convert(row("3d82b9e1-fd14-8035-a065-e6b0e6f49e90", "OP6 출근",
                date("2026-09-24T21:00:00.000Z", null), null));

        assertThat(e.externalId()).isEqualTo("3d82b9e1fd148035a065e6b0e6f49e90");
        assertThat(e.startAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 6, 0));
        assertThat(e.endAt()).isNull();
        assertThat(e.allDay()).isFalse();
        assertThat(e.location()).isNull();
    }

    @Test
    void offsetEndAndLocationAreKept() {
        ExternalEvent e = convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", " 리마 2차 시험 ",
                date("2026-09-12T13:50:00.000+09:00", "2026-09-12T06:40:00.000Z"), "신라중학교"));

        assertThat(e.title()).isEqualTo("리마 2차 시험");
        assertThat(e.startAt()).isEqualTo(LocalDateTime.of(2026, 9, 12, 13, 50));
        assertThat(e.endAt()).isEqualTo(LocalDateTime.of(2026, 9, 12, 15, 40));
        assertThat(e.location()).isEqualTo("신라중학교");
    }

    @Test
    void endEqualToStartIsDropped() {
        ExternalEvent e = convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "수강취소 마감",
                date("2026-09-29T00:00:00.000Z", "2026-09-29T00:00:00.000Z"), null));
        assertThat(e.endAt()).isNull();
    }

    @Test
    void dateOnlyIsAllDayAndRangeEndsNextMidnight() {
        ExternalEvent single = convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "빈첸 콘서트", date("2025-12-14", null), null));
        assertThat(single.allDay()).isTrue();
        assertThat(single.startAt()).isEqualTo(LocalDateTime.of(2025, 12, 14, 0, 0));
        assertThat(single.endAt()).isNull();

        ExternalEvent range = convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "기말고사", date("2026-12-14", "2026-12-20"), null));
        assertThat(range.endAt()).isEqualTo(LocalDateTime.of(2026, 12, 21, 0, 0));
    }

    @Test
    void rowsWithoutTitleOrDateAreSkipped() {
        assertThat(convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "  ", date("2026-01-01", null), null))).isNull();
        assertThat(convert(row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "날짜 없음", null, null))).isNull();
    }
}
