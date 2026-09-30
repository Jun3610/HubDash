package com.junyoung.dashboard.domain.sync.source;

import com.junyoung.dashboard.domain.integration.service.CredentialStore;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionIds;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 노션 Schedule DB → 일정 (이슈 #228). 변환은 노션 이관(#94)과 같게:
 * 날짜만 있으면 하루 종일(기간이면 끝은 마지막 날 다음 날 0시), 시각이 있으면 UTC → KST,
 * 끝이 없거나 시작과 같으면 end_at은 비운다. Status는 가져오지 않는다.
 */
@Component
public class NotionScheduleSource implements SyncSource {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String DATE_PROPERTY = "Time Slot";
    private static final String LOCATION_PROPERTY = "Location";

    private final NotionClient notion;
    private final CredentialStore credentials;

    public NotionScheduleSource(NotionClient notion, CredentialStore credentials) {
        this.notion = notion;
        this.credentials = credentials;
    }

    @Override
    public EventSource source() {
        return EventSource.NOTION;
    }

    @Override
    public boolean configured() {
        return notion.configured() && credentials.notionSettings().scheduleDatabaseId() != null;
    }

    @Override
    public List<ExternalEvent> fetch() {
        String databaseId = credentials.notionSettings().scheduleDatabaseId();
        List<ExternalEvent> events = new ArrayList<>();
        for (Map<?, ?> row : notion.queryDatabaseRows(databaseId)) {
            ExternalEvent event = convert(row);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    /** 제목이나 날짜가 없는 행은 일정이 될 수 없어 건너뛴다(null) */
    static ExternalEvent convert(Map<?, ?> row) {
        Map<?, ?> props = (Map<?, ?>) row.get("properties");
        String id = NotionIds.extract(String.valueOf(row.get("id")));
        if (props == null || id == null) {
            return null;
        }
        String title = null;
        Map<?, ?> date = null;
        String location = null;
        for (Map.Entry<?, ?> e : props.entrySet()) {
            Map<?, ?> p = (Map<?, ?>) e.getValue();
            String type = String.valueOf(p.get("type"));
            if ("title".equals(type)) {
                title = NotionClient.plainText((List<?>) p.get("title"));
            } else if ("date".equals(type) && p.get("date") instanceof Map<?, ?> d
                    && (date == null || DATE_PROPERTY.equals(e.getKey()))) {
                date = d;
            } else if ("rich_text".equals(type) && LOCATION_PROPERTY.equals(e.getKey())) {
                location = NotionClient.plainText((List<?>) p.get("rich_text"));
            }
        }
        if (title == null || title.isBlank() || date == null || date.get("start") == null) {
            return null;
        }
        String start = String.valueOf(date.get("start"));
        String end = date.get("end") == null ? null : String.valueOf(date.get("end"));
        String zone = date.get("time_zone") == null ? null : String.valueOf(date.get("time_zone"));
        try {
            return start.length() == 10
                    ? allDay(id, title, start, end, location)
                    : timed(id, title, start, end, zone, location);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static ExternalEvent allDay(String id, String title, String start, String end, String location) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = end == null ? s : LocalDate.parse(end.substring(0, 10));
        // 하루짜리는 끝을 비우고, 여러 날이면 끝은 마지막 날 다음 날 0시(이관 때와 같은 배타적 끝)
        LocalDateTime endAt = e.isAfter(s) ? e.plusDays(1).atStartOfDay() : null;
        return new ExternalEvent(id, clip(title, 200), s.atStartOfDay(), endAt, clip(location, 200), true);
    }

    private static ExternalEvent timed(String id, String title, String start, String end, String zone, String location) {
        LocalDateTime s = toKst(start, zone);
        LocalDateTime e = end == null ? null : toKst(end, zone);
        if (e != null && !e.isAfter(s)) {
            e = null;
        }
        return new ExternalEvent(id, clip(title, 200), s, e, clip(location, 200), false);
    }

    /** "2026-09-24T21:00:00.000Z"나 "+09:00"이 붙은 값은 그 시각을 KST로, 시간대 없는 값은 time_zone(없으면 KST) 기준 */
    static LocalDateTime toKst(String value, String zone) {
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(KST).toLocalDateTime();
        } catch (DateTimeParseException noOffset) {
            ZoneId z = zone == null ? KST : ZoneId.of(zone);
            return LocalDateTime.parse(value).atZone(z).withZoneSameInstant(KST).toLocalDateTime();
        }
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
