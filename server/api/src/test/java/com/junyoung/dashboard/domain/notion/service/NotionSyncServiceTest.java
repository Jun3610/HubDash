package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.notion.client.NotionClient;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncResult;
import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

    @Mock
    private NotionClient notionClient;
    @Mock
    private NotionSyncSourceRepository sourceRepository;
    @Mock
    private NotionSourceSynchronizer synchronizer;
    @InjectMocks
    private NotionSyncService syncService;

    @Test
    void failedSourceIsReportedAndOtherSourcesStillSync() {
        NotionSyncSource broken = source(1L, "broken");
        NotionSyncSource ok = source(2L, "ok");
        when(sourceRepository.findAll()).thenReturn(List.of(broken, ok));
        // 통합이 연결되지 않은 페이지를 조회하면 노션이 404를 준다
        when(synchronizer.sync(1L)).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null));
        when(synchronizer.sync(2L)).thenReturn(NotionSyncResult.success(2L, "ok", 3, 0, 0));

        List<NotionSyncResult> results = syncService.syncAll();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).error()).contains("404");
        assertThat(results.get(1).created()).isEqualTo(3);
    }

    @Test
    void enabledOnlyWhenClientConfigured() {
        when(notionClient.isConfigured()).thenReturn(false);
        assertThat(syncService.isEnabled()).isFalse();
    }

    private static NotionSyncSource source(Long id, String notionId) {
        NotionSyncSource source = new NotionSyncSource(notionId, NotionSourceType.DATABASE, new HubCategory("c", null));
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }
}
