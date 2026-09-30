package com.junyoung.dashboard.domain.sync.source;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * iCalendar(.ics) 원문 → 일정 (이슈 #229). CalDAV로 받은 리소스 하나에는 일정 하나와, 반복 일정이면 개별 수정된 회차가 함께 들어 있다.
 * 반복 일정(RRULE/RDATE, EXDATE 제외)은 기간 안에서 펼치고 원본 ID는 "UID@발생 시각"으로 한다.
 * 개별 수정된 회차(RECURRENCE-ID)는 펼친 같은 회차를 대신한다. 취소된 일정(STATUS:CANCELLED)은 뺀다.
 * 시간은 KST로, 날짜만 있는 일정은 하루 종일(여러 날이면 끝은 마지막 날 다음 날 0시 = DTEND 그대로).
 */
public final class ICalendarEvents {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter UTC_KEY = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FLOATING_KEY = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.BASIC_ISO_DATE;

    private ICalendarEvents() {
    }

    public static List<ExternalEvent> parse(String ics, Instant from, Instant to) {
        Calendar calendar;
        try {
            calendar = new CalendarBuilder().build(new StringReader(ics));
        } catch (IOException | ParserException e) {
            return List.of(); // 읽을 수 없는 리소스 하나 때문에 전체를 멈추지 않는다
        }
        List<VEvent> masters = new ArrayList<>();
        Map<String, VEvent> overrides = new HashMap<>();
        for (Component c : calendar.getComponents(Component.VEVENT)) {
            VEvent e = (VEvent) c;
            if (e.getRecurrenceId() != null) {
                overrides.put(uid(e) + "@" + key(e.getRecurrenceId().getDate()), e);
            } else {
                masters.add(e);
            }
        }
        List<ExternalEvent> out = new ArrayList<>();
        for (VEvent master : masters) {
            String uid = uid(master);
            if (uid == null || master.getDateTimeStart() == null) {
                continue;
            }
            if (!recurring(master)) {
                add(out, uid, master, master.getDateTimeStart().getDate());
                continue;
            }
            for (Period<Temporal> occurrence : master.<Temporal>calculateRecurrenceSet(window(master, from, to))) {
                String id = uid + "@" + key(occurrence.getStart());
                VEvent override = overrides.remove(id);
                if (override != null) {
                    add(out, id, override, override.getDateTimeStart() == null ? occurrence.getStart() : override.getDateTimeStart().getDate());
                } else {
                    add(out, id, master, occurrence.getStart());
                }
            }
        }
        // 펼친 회차에 없던 개별 수정 회차(기간 밖 원본을 기간 안으로 옮긴 경우)
        for (Map.Entry<String, VEvent> e : overrides.entrySet()) {
            VEvent o = e.getValue();
            if (o.getDateTimeStart() != null) {
                add(out, e.getKey(), o, o.getDateTimeStart().getDate());
            }
        }
        return out;
    }

    /** 반복을 펼칠 기간 — ical4j는 기간의 타입이 DTSTART의 타입(날짜, 시간대 없는 시각, 시간대 있는 시각)과 같아야 한다 */
    private static Period<? extends Temporal> window(VEvent master, Instant from, Instant to) {
        Temporal start = master.getDateTimeStart().getDate();
        if (start instanceof LocalDate) {
            return new Period<>(from.atZone(KST).toLocalDate(), to.atZone(KST).toLocalDate());
        }
        if (start instanceof LocalDateTime) {
            return new Period<>(from.atZone(KST).toLocalDateTime(), to.atZone(KST).toLocalDateTime());
        }
        return new Period<>(from.atZone(ZoneOffset.UTC), to.atZone(ZoneOffset.UTC));
    }

    private static void add(List<ExternalEvent> out, String id, VEvent event, Temporal occurrenceStart) {
        if (cancelled(event)) {
            return;
        }
        String title = text(event.getProperty(Property.SUMMARY));
        if (title == null) {
            title = "(제목 없음)";
        }
        Temporal originalStart = event.getDateTimeStart().getDate();
        Temporal originalEnd = event.getDateTimeEnd() == null ? null : event.getDateTimeEnd().getDate();
        String location = text(event.getProperty(Property.LOCATION));
        if (occurrenceStart instanceof LocalDate day) {
            long days = originalEnd instanceof LocalDate end ? ChronoUnit.DAYS.between((LocalDate) originalStart, end) : 1;
            LocalDateTime endAt = days > 1 ? day.plusDays(days).atStartOfDay() : null;
            out.add(new ExternalEvent(id, clip(title), day.atStartOfDay(), endAt, clip(location), true));
            return;
        }
        LocalDateTime start = toKst(occurrenceStart);
        LocalDateTime endAt = null;
        Duration length = length(event, originalStart, originalEnd);
        if (length != null && !length.isZero() && !length.isNegative()) {
            endAt = start.plus(length);
        }
        out.add(new ExternalEvent(id, clip(title), start, endAt, clip(location), false));
    }

    private static Duration length(VEvent event, Temporal start, Temporal end) {
        if (end != null && !(end instanceof LocalDate)) {
            return Duration.between(toKst(start), toKst(end));
        }
        Optional<Property> duration = event.getProperty(Property.DURATION);
        if (duration.isPresent()) {
            try {
                return Duration.parse(duration.get().getValue());
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    static LocalDateTime toKst(Temporal t) {
        if (t instanceof ZonedDateTime z) {
            return z.withZoneSameInstant(KST).toLocalDateTime();
        }
        if (t instanceof OffsetDateTime o) {
            return o.atZoneSameInstant(KST).toLocalDateTime();
        }
        if (t instanceof Instant i) {
            return i.atZone(KST).toLocalDateTime();
        }
        if (t instanceof LocalDateTime l) {
            return l; // 시간대 없는(floating) 시각은 한국 시각으로 본다
        }
        if (t instanceof LocalDate d) {
            return d.atStartOfDay();
        }
        throw new IllegalArgumentException("알 수 없는 시각: " + t);
    }

    /** 회차 구분 키 — 같은 순간이면 시간대 표기가 달라도 같은 키 */
    static String key(Temporal t) {
        if (t instanceof LocalDate d) {
            return DATE_KEY.format(d);
        }
        if (t instanceof LocalDateTime l) {
            return FLOATING_KEY.format(l);
        }
        if (t instanceof ZonedDateTime z) {
            return UTC_KEY.format(z.toInstant());
        }
        if (t instanceof OffsetDateTime o) {
            return UTC_KEY.format(o.toInstant());
        }
        return UTC_KEY.format((Instant) t);
    }

    private static boolean recurring(VEvent e) {
        return e.getProperty(Property.RRULE).isPresent() || e.getProperty(Property.RDATE).isPresent();
    }

    private static boolean cancelled(VEvent e) {
        return e.getProperty(Property.STATUS).map(p -> "CANCELLED".equalsIgnoreCase(p.getValue())).orElse(false);
    }

    private static String uid(VEvent e) {
        return text(e.getProperty(Property.UID));
    }

    private static String text(Optional<Property> p) {
        return p.map(Property::getValue).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
    }

    private static String clip(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 200 ? s : s.substring(0, 200);
    }
}
