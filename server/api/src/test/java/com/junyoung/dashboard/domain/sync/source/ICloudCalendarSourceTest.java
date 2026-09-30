package com.junyoung.dashboard.domain.sync.source;

import com.junyoung.dashboard.domain.integration.service.CredentialStore;
import com.junyoung.dashboard.domain.integration.service.ICloudSettings;
import com.junyoung.dashboard.global.icloud.CalDavCalendar;
import com.junyoung.dashboard.global.icloud.CalDavClient;
import com.junyoung.dashboard.global.icloud.ICloudException;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ICloudCalendarSourceTest {

    private static final ICloudLogin LOGIN = new ICloudLogin("me@icloud.com", "abcd-efgh-ijkl-mnop");
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    private final CalDavClient caldav = mock(CalDavClient.class);
    private final CredentialStore store = mock(CredentialStore.class);
    private final ICloudCalendarSource source = new ICloudCalendarSource(caldav, store, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void readsOnlyChosenCalendarsWithinOneYearEachWay() {
        when(store.icloudLogin()).thenReturn(LOGIN);
        when(store.icloudSettings()).thenReturn(new ICloudSettings("me@icloud.com", List.of("집")));
        when(caldav.calendars(LOGIN)).thenReturn(List.of(
                new CalDavCalendar("집", "https://x/home/"),
                new CalDavCalendar("ParkJunYoung_Schedule", "https://x/pj/")));
        when(caldav.events(eq(LOGIN), eq("https://x/home/"), any(), any())).thenReturn(List.of("""
                BEGIN:VCALENDAR\r
                VERSION:2.0\r
                BEGIN:VEVENT\r
                UID:A\r
                DTSTART:20261001T010000Z\r
                SUMMARY:장보기\r
                END:VEVENT\r
                END:VCALENDAR\r
                """));

        assertThat(source.configured()).isTrue();
        assertThat(source.fetch()).extracting(ExternalEvent::externalId, ExternalEvent::title)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("A", "장보기"));
        verify(caldav).events(LOGIN, "https://x/home/", Instant.parse("2025-09-30T00:00:00Z"), Instant.parse("2027-09-30T00:00:00Z"));
        verify(caldav, never()).events(any(), eq("https://x/pj/"), any(), any());
    }

    @Test
    void notConfiguredWithoutLoginOrWithoutChosenCalendars() {
        when(store.icloudLogin()).thenReturn(null);
        assertThat(source.configured()).isFalse();

        when(store.icloudLogin()).thenReturn(LOGIN);
        when(store.icloudSettings()).thenReturn(new ICloudSettings("me@icloud.com", List.of()));
        assertThat(source.configured()).isFalse();
    }

    @Test
    void chosenCalendarThatNoLongerExistsIsAnError() {
        when(store.icloudLogin()).thenReturn(LOGIN);
        when(store.icloudSettings()).thenReturn(new ICloudSettings("me@icloud.com", List.of("지운 캘린더")));
        when(caldav.calendars(LOGIN)).thenReturn(List.of(new CalDavCalendar("집", "https://x/home/")));

        assertThatThrownBy(source::fetch).isInstanceOf(ICloudException.class).hasMessageContaining("지운 캘린더");
    }
}
