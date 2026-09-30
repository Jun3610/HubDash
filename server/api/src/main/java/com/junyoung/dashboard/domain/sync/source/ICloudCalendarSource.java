package com.junyoung.dashboard.domain.sync.source;

import com.junyoung.dashboard.domain.integration.service.CredentialStore;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.global.exception.IntegrationException;
import com.junyoung.dashboard.global.icloud.CalDavCalendar;
import com.junyoung.dashboard.global.icloud.CalDavClient;
import com.junyoung.dashboard.global.icloud.ICloudException;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * iCloud 캘린더 → 일정 (이슈 #229). 설정에서 고른 캘린더만, 1년 전 ~ 1년 뒤 일정을 CalDAV로 읽는다.
 * 반복 일정은 이 기간 안에서 펼친다(ICalendarEvents).
 */
@Component
@Order(2)
public class ICloudCalendarSource implements SyncSource {

    private static final long WINDOW_DAYS = 365;

    private final CalDavClient caldav;
    private final CredentialStore credentials;
    private final Clock clock;

    @Autowired
    public ICloudCalendarSource(CalDavClient caldav, CredentialStore credentials) {
        this(caldav, credentials, Clock.systemUTC());
    }

    ICloudCalendarSource(CalDavClient caldav, CredentialStore credentials, Clock clock) {
        this.caldav = caldav;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    public EventSource source() {
        return EventSource.ICLOUD;
    }

    /** 계정이 저장돼 있고 캘린더를 하나 이상 골랐을 때만 */
    @Override
    public boolean configured() {
        try {
            return credentials.icloudLogin() != null && !credentials.icloudSettings().calendars().isEmpty();
        } catch (IntegrationException unreadable) {
            return false; // 암호화 키가 바뀌어 앱 암호를 못 풂 — 설정 화면에서 다시 입력
        }
    }

    @Override
    public List<ExternalEvent> fetch() {
        ICloudLogin login = credentials.icloudLogin();
        List<String> wanted = credentials.icloudSettings().calendars();
        List<CalDavCalendar> selected = caldav.calendars(login).stream()
                .filter(c -> wanted.contains(c.name()))
                .toList();
        if (selected.isEmpty()) {
            throw new ICloudException("고른 캘린더(" + String.join(", ", wanted) + ")를 iCloud에서 찾을 수 없어요 — 설정에서 캘린더를 다시 골라 주세요");
        }
        Instant now = clock.instant();
        Instant from = now.minus(WINDOW_DAYS, ChronoUnit.DAYS);
        Instant to = now.plus(WINDOW_DAYS, ChronoUnit.DAYS);
        List<ExternalEvent> events = new ArrayList<>();
        for (CalDavCalendar calendar : selected) {
            for (String ics : caldav.events(login, calendar.url(), from, to)) {
                events.addAll(ICalendarEvents.parse(ics, from, to));
            }
        }
        return events;
    }
}
