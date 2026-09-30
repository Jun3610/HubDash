package com.junyoung.dashboard.domain.sync;

import com.junyoung.dashboard.domain.integration.entity.IntegrationCredential;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import com.junyoung.dashboard.domain.integration.repository.IntegrationCredentialRepository;
import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.sync.repository.SyncRunRepository;
import com.junyoung.dashboard.global.notion.NotionClient;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 갱신 버튼 경로 전체: 설정(DB) → 노션(가짜) → 변환 → 백필·upsert → 기록 → 상태 (이슈 #228, #230)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncApiTest {

    private static final String DB = "3c62b9e1fd1480778cc7eb24389c1704";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private IntegrationCredentialRepository credentials;
    @Autowired
    private EventRepository events;
    @Autowired
    private SyncRunRepository runs;
    @MockitoBean
    private NotionClient notion;

    @BeforeEach
    void setUp() {
        runs.deleteAll();
        events.deleteAll();
        credentials.deleteAll();
        IntegrationCredential c = new IntegrationCredential(IntegrationProvider.NOTION);
        c.save("{\"scheduleDatabaseId\":\"" + DB + "\"}", null, null);
        credentials.save(c);
        when(notion.configured()).thenReturn(true);
    }

    private static Map<String, Object> row(String id, String title, String start) {
        return Map.of("id", id, "properties", Map.of(
                "Event", Map.of("type", "title", "title", List.of(Map.of("plain_text", title))),
                "Time Slot", Map.of("type", "date", "date", Map.of("start", start))));
    }

    @Test
    void syncsNotionScheduleIdempotentlyAndBackfillsMigratedEvents() throws Exception {
        // 노션에서 이관해 둔 일정(출처 없음) 하나와, 노션과 무관한 직접 일정 하나
        Event migrated = events.save(new Event("OP6 출근", LocalDateTime.of(2026, 9, 25, 6, 0),
                LocalDateTime.of(2026, 9, 25, 7, 0), null, null, false));
        Event mine = events.save(new Event("내가 만든 일정", LocalDateTime.of(2026, 9, 25, 6, 0), null, null, null, false));
        List<Map<?, ?>> rows = List.of(
                row("3d82b9e1-fd14-8035-a065-e6b0e6f49e90", "OP6 출근", "2026-09-24T21:00:00.000Z"),
                row("3d82b9e1-fd14-80c1-99b3-fddd2303a25c", "새 일정", "2026-10-01"));
        when(notion.queryDatabaseRows(DB)).thenReturn(rows);

        mvc.perform(post("/api/sync").header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sources[0].name").value("NOTION"))
                .andExpect(jsonPath("$.data.sources[0].ok").value(true))
                .andExpect(jsonPath("$.data.sources[0].added").value(1))
                .andExpect(jsonPath("$.data.sources[0].updated").value(1))
                .andExpect(jsonPath("$.data.sources[0].deleted").value(0))
                .andExpect(jsonPath("$.data.sources[1].name").value("NOTION_HUB"));

        assertThat(events.findById(migrated.getId()).orElseThrow().getExternalId()).isEqualTo("3d82b9e1fd148035a065e6b0e6f49e90");
        assertThat(events.findById(mine.getId()).orElseThrow().getSource()).isEqualTo(EventSource.MANUAL);
        assertThat(events.count()).isEqualTo(3);

        // 몇 번을 눌러도 중복 없음
        mvc.perform(post("/api/sync").header("X-API-KEY", "test-api-key"))
                .andExpect(jsonPath("$.data.sources[0].added").value(0))
                .andExpect(jsonPath("$.data.sources[0].updated").value(0));
        assertThat(events.count()).isEqualTo(3);

        mvc.perform(get("/api/sync/status").header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false))
                .andExpect(jsonPath("$.data.sources[0].name").value("NOTION"))
                .andExpect(jsonPath("$.data.sources[0].configured").value(true))
                .andExpect(jsonPath("$.data.sources[0].lastRun.ok").value(true))
                .andExpect(jsonPath("$.data.sources[0].lastRun.added").value(0));
    }
}
