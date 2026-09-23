package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.hub.dto.RawHubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.RawHubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.domain.hub.service.RawHubLinkService;
import com.junyoung.dashboard.domain.notion.client.NotionClient;
import com.junyoung.dashboard.domain.notion.client.NotionPage;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncResult;
import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncedPage;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncedPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotionSourceSynchronizerTest {

    private static final String NEW_PAGE = "11111111111111111111111111111111";
    private static final String MIGRATED_PAGE = "22222222222222222222222222222222";
    private static final String SYNCED_PAGE = "33333333333333333333333333333333";
    private static final String UNTITLED_PAGE = "44444444444444444444444444444444";

    @Mock
    private NotionClient notionClient;
    @Mock
    private NotionSyncSourceRepository sourceRepository;
    @Mock
    private NotionSyncedPageRepository syncedPageRepository;
    @Mock
    private HubLinkRepository hubLinkRepository;
    @Mock
    private RawHubLinkService rawHubLinkService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private NotionSourceSynchronizer synchronizer;
    private HubCategory category;
    private NotionSyncSource source;

    @BeforeEach
    void setUp() {
        synchronizer = new NotionSourceSynchronizer(notionClient, sourceRepository, syncedPageRepository,
                hubLinkRepository, rawHubLinkService, clock);
        category = new HubCategory("Docker", null);
        ReflectionTestUtils.setField(category, "id", 7L);
        source = new NotionSyncSource("db1", NotionSourceType.DATABASE, category);
        ReflectionTestUtils.setField(source, "id", 1L);
        lenient().when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));
    }

    @Test
    void createsRawLinkOnlyForNewTitledPagesAndRecordsThem() {
        when(notionClient.listDatabasePages("db1")).thenReturn(List.of(
                new NotionPage(NEW_PAGE, "  새 페이지 "),
                new NotionPage(MIGRATED_PAGE, "이관된 페이지"),
                new NotionPage(SYNCED_PAGE, "이미 동기화됨"),
                new NotionPage(UNTITLED_PAGE, " ")));
        when(syncedPageRepository.findNotionPageIdsBySourceId(1L)).thenReturn(new HashSet<>(Set.of(SYNCED_PAGE)));
        when(hubLinkRepository.findByCategoryId(7L)).thenReturn(List.of(
                new HubLink(category, "이관된 페이지", "https://app.notion.com/p/" + MIGRATED_PAGE, null),
                new HubLink(category, "노션 아닌 링크", "https://example.com", null)));
        when(rawHubLinkService.create(any())).thenReturn(rawResponse(99L));

        NotionSyncResult result = synchronizer.sync(1L);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.error()).isNull();

        ArgumentCaptor<RawHubLinkRequest> request = ArgumentCaptor.forClass(RawHubLinkRequest.class);
        verify(rawHubLinkService).create(request.capture());
        assertThat(request.getValue()).isEqualTo(new RawHubLinkRequest(
                "7", "새 페이지", "https://app.notion.com/p/" + NEW_PAGE, NotionSourceSynchronizer.LINK_DESCRIPTION));

        ArgumentCaptor<NotionSyncedPage> saved = ArgumentCaptor.forClass(NotionSyncedPage.class);
        verify(syncedPageRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(NotionSyncedPage::getNotionPageId, NotionSyncedPage::getRawHubLinkId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(NEW_PAGE, 99L),
                        org.assertj.core.groups.Tuple.tuple(MIGRATED_PAGE, null));
        assertThat(source.getLastSyncedAt()).isEqualTo(LocalDateTime.of(2026, 9, 23, 10, 0));
    }

    @Test
    void pageSourceListsChildPages() {
        NotionSyncSource pageSource = new NotionSyncSource("parent", NotionSourceType.PAGE, category);
        ReflectionTestUtils.setField(pageSource, "id", 2L);
        when(sourceRepository.findById(2L)).thenReturn(Optional.of(pageSource));
        when(notionClient.listChildPages("parent")).thenReturn(List.of());
        when(syncedPageRepository.findNotionPageIdsBySourceId(2L)).thenReturn(new HashSet<>());
        when(hubLinkRepository.findByCategoryId(7L)).thenReturn(List.of());

        NotionSyncResult result = synchronizer.sync(2L);

        assertThat(result.created()).isZero();
        verify(notionClient, never()).listDatabasePages(any());
    }

    @Test
    void samePageListedTwiceIsCreatedOnce() {
        when(notionClient.listDatabasePages("db1")).thenReturn(List.of(
                new NotionPage(NEW_PAGE, "중복"), new NotionPage(NEW_PAGE, "중복")));
        when(syncedPageRepository.findNotionPageIdsBySourceId(1L)).thenReturn(new HashSet<>());
        when(hubLinkRepository.findByCategoryId(7L)).thenReturn(List.of());
        when(rawHubLinkService.create(any())).thenReturn(rawResponse(1L));

        assertThat(synchronizer.sync(1L).created()).isEqualTo(1);
    }

    @Test
    void truncatesTitleToRawColumnWidth() {
        when(notionClient.listDatabasePages("db1")).thenReturn(List.of(new NotionPage(NEW_PAGE, "가".repeat(250))));
        when(syncedPageRepository.findNotionPageIdsBySourceId(1L)).thenReturn(new HashSet<>());
        when(hubLinkRepository.findByCategoryId(7L)).thenReturn(List.of());
        when(rawHubLinkService.create(any())).thenReturn(rawResponse(1L));

        synchronizer.sync(1L);

        ArgumentCaptor<RawHubLinkRequest> request = ArgumentCaptor.forClass(RawHubLinkRequest.class);
        verify(rawHubLinkService).create(request.capture());
        assertThat(request.getValue().titleRaw()).hasSize(200);
    }

    private static RawHubLinkResponse rawResponse(Long id) {
        return new RawHubLinkResponse(id, "7", "t", "u", null, "PENDING", null, null, null);
    }
}
