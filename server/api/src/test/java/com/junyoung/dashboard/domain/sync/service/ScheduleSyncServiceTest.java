package com.junyoung.dashboard.domain.sync.service;

import com.junyoung.dashboard.domain.hub.service.HubNotionSyncService;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.sync.dto.SyncResponse;
import com.junyoung.dashboard.domain.sync.dto.SyncSourceResult;
import com.junyoung.dashboard.domain.sync.entity.SyncRun;
import com.junyoung.dashboard.domain.sync.repository.SyncRunRepository;
import com.junyoung.dashboard.domain.sync.source.ExternalEvent;
import com.junyoung.dashboard.domain.sync.source.SyncSource;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleSyncServiceTest {

    private final SyncSource notionSource = mock(SyncSource.class);
    private final SyncSource icloudSource = mock(SyncSource.class);
    private final EventImporter importer = mock(EventImporter.class);
    private final SyncRunRepository runs = mock(SyncRunRepository.class);
    private final HubNotionSyncService hub = mock(HubNotionSyncService.class);
    private final NotionClient notion = mock(NotionClient.class);
    private final ScheduleSyncService service = new ScheduleSyncService(List.of(notionSource, icloudSource), importer, runs,
            hub, notion, Clock.fixed(Instant.parse("2026-09-30T01:00:00Z"), ZoneId.of("Asia/Seoul")));

    {
        when(notionSource.source()).thenReturn(EventSource.NOTION);
        when(notionSource.name()).thenReturn("NOTION");
        when(icloudSource.source()).thenReturn(EventSource.ICLOUD);
        when(icloudSource.name()).thenReturn("ICLOUD");
    }

    @Test
    void failingSourceIsReportedAndOthersStillRun() {
        when(notionSource.configured()).thenReturn(true);
        when(icloudSource.configured()).thenReturn(true);
        when(notionSource.fetch()).thenThrow(new NotionException("노션 DB를 읽을 수 없어요 — 노션에서 이 DB를 HubDash 통합에 연결해 주세요"));
        List<ExternalEvent> icloud = List.of(new ExternalEvent("u", "t", null, null, null, false));
        when(icloudSource.fetch()).thenReturn(icloud);
        when(importer.apply(EventSource.ICLOUD, icloud, true)).thenReturn(new EventImporter.Counts(1, 0, 0, 0));

        SyncResponse response = service.syncAll();

        assertThat(response.sources()).containsExactly(
                SyncSourceResult.failed("NOTION", "노션 DB를 읽을 수 없어요 — 노션에서 이 DB를 HubDash 통합에 연결해 주세요"),
                new SyncSourceResult("ICLOUD", true, 1, 0, 0, null));
        ArgumentCaptor<SyncRun> saved = ArgumentCaptor.forClass(SyncRun.class);
        verify(runs, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(SyncRun::getSource, SyncRun::isOk)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("NOTION", false),
                        org.assertj.core.groups.Tuple.tuple("ICLOUD", true));
    }

    @Test
    void unconfiguredSourcesAreSkipped() {
        when(notionSource.configured()).thenReturn(false);
        when(icloudSource.configured()).thenReturn(false);

        assertThat(service.syncAll().sources()).isEmpty();
        verify(notionSource, never()).fetch();
        verify(runs, never()).save(any());
    }

    @Test
    void backfillOnlyOnFirstSuccessfulNotionSync() {
        when(notionSource.configured()).thenReturn(true);
        when(notionSource.fetch()).thenReturn(List.of());
        when(importer.apply(any(), any(), anyBoolean())).thenReturn(new EventImporter.Counts(0, 0, 0, 0));

        when(runs.existsBySourceAndOkTrue("NOTION")).thenReturn(false);
        service.syncAll();
        verify(importer).apply(EventSource.NOTION, List.of(), true);

        when(runs.existsBySourceAndOkTrue("NOTION")).thenReturn(true);
        service.syncAll();
        verify(importer).apply(EventSource.NOTION, List.of(), false);
    }

    // iCloud가 일정 원본(#234) — 노션 일정을 옮겨 둔 캘린더를 나중에 켜도 중복되지 않게 매번 짝짓는다
    @Test
    void icloudBackfillsOnEverySync() {
        when(icloudSource.configured()).thenReturn(true);
        when(icloudSource.fetch()).thenReturn(List.of());
        when(importer.apply(any(), any(), anyBoolean())).thenReturn(new EventImporter.Counts(0, 0, 0, 0));
        when(runs.existsBySourceAndOkTrue("ICLOUD")).thenReturn(true);

        service.syncAll();
        service.syncAll();

        verify(importer, times(2)).apply(eq(EventSource.ICLOUD), any(), eq(true));
    }

    @Test
    void hubLinksAreIncludedWhenNotionIsConnected() {
        when(notion.configured()).thenReturn(true);
        when(hub.sync()).thenReturn(new HubNotionSyncService.SyncResult(3, List.of()));

        assertThat(service.syncAll().sources())
                .containsExactly(new SyncSourceResult("NOTION_HUB", true, 3, 0, 0, null));
    }
}
