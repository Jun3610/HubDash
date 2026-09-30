package com.junyoung.dashboard.domain.sync;

import com.junyoung.dashboard.domain.integration.service.CredentialStore;
import com.junyoung.dashboard.domain.integration.service.ICloudSettings;
import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.sync.repository.SyncRunRepository;
import com.junyoung.dashboard.global.icloud.CalDavCalendar;
import com.junyoung.dashboard.global.icloud.CalDavClient;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// iCloud가 일정 원본(#234): 노션 일정을 옮겨 둔 캘린더를 켜도 HubDash에 이미 있는 이관 일정과 중복되지 않는다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ICloudBackfillTest {

    private static final ICloudLogin LOGIN = new ICloudLogin("me@icloud.com", "abcd-efgh-ijkl-mnop");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EventRepository events;
    @Autowired
    private SyncRunRepository runs;
    @MockitoBean
    private CalDavClient caldav;
    @MockitoBean
    private CredentialStore credentials;

    private static String ics(String uid, String title, String dtstart) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:" + uid + "\r\nDTSTART:" + dtstart
                + "\r\nSUMMARY:" + title + "\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
    }

    @BeforeEach
    void setUp() {
        runs.deleteAll();
        events.deleteAll();
        when(credentials.icloudLogin()).thenReturn(LOGIN);
    }

    @Test
    void calendarAddedLaterStillAdoptsExistingEventsInsteadOfDuplicating() throws Exception {
        // 노션에서 이관해 HubDash에 있던 일정 (출처 없음)
        Event migrated = events.save(new Event("OP6 출근", LocalDateTime.of(2026, 9, 25, 6, 0),
                LocalDateTime.of(2026, 9, 25, 7, 0), null, null, false));
        when(caldav.calendars(LOGIN)).thenReturn(List.of(
                new CalDavCalendar("집", "https://x/home/"),
                new CalDavCalendar("ParkJunYoung_Schedule", "https://x/pj/")));
        when(caldav.events(eq(LOGIN), eq("https://x/home/"), any(), any()))
                .thenReturn(List.of(ics("HOME-1", "장보기", "20261001T010000Z")));
        when(caldav.events(eq(LOGIN), eq("https://x/pj/"), any(), any()))
                .thenReturn(List.of(ics("PJ-1", "OP6 출근", "20260924T210000Z"), ics("PJ-2", "iCloud에만 있는 일정", "20261002T010000Z")));

        // 처음엔 "집"만 켬
        when(credentials.icloudSettings()).thenReturn(new ICloudSettings("me@icloud.com", List.of("집")));
        mvc.perform(post("/api/sync").header("X-API-KEY", "test-api-key"))
                .andExpect(jsonPath("$.data.sources[0].name").value("ICLOUD"))
                .andExpect(jsonPath("$.data.sources[0].added").value(1));

        // 나중에 노션 일정을 옮겨 둔 캘린더를 켬 → 이관 일정은 짝지어지고 새 일정만 추가
        when(credentials.icloudSettings()).thenReturn(new ICloudSettings("me@icloud.com", List.of("집", "ParkJunYoung_Schedule")));
        mvc.perform(post("/api/sync").header("X-API-KEY", "test-api-key"))
                .andExpect(jsonPath("$.data.sources[0].ok").value(true))
                .andExpect(jsonPath("$.data.sources[0].added").value(1))
                .andExpect(jsonPath("$.data.sources[0].updated").value(1));

        Event adopted = events.findById(migrated.getId()).orElseThrow();
        assertThat(adopted.getSource()).isEqualTo(EventSource.ICLOUD);
        assertThat(adopted.getExternalId()).isEqualTo("PJ-1");
        assertThat(events.count()).isEqualTo(3);
    }
}
