package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionPage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HubNotionSyncServiceTest {

    private final NotionClient notion = mock(NotionClient.class);
    private final HubCategoryRepository categories = mock(HubCategoryRepository.class);
    private final HubLinkRepository links = mock(HubLinkRepository.class);
    private final HubNotionSyncService service = new HubNotionSyncService(notion, categories, links);

    @Test
    void addsOnlyPagesNotYetLinked() {
        HubCategory docker = new HubCategory("Docker", null, "66d2b9e1fd1482abb73a01f2f39c987c");
        HubCategory memo = new HubCategory("문서", null, null); // 노션 DB가 없으면 건너뜀
        when(categories.findAll()).thenReturn(List.of(docker, memo));
        when(links.findAllUrls()).thenReturn(List.of("https://app.notion.com/p/3532b9e1fd1480ffb994fe9eb920f8c3"));
        when(notion.queryDatabase("66d2b9e1fd1482abb73a01f2f39c987c")).thenReturn(List.of(
                new NotionPage("3532b9e1-fd14-80ff-b994-fe9eb920f8c3", "개념", "완료", "2026-06-03"),
                new NotionPage("aaaabbbb-cccc-dddd-eeee-ffff00001111", "새 글", "진행 중", null),
                new NotionPage("aaaabbbb-cccc-dddd-eeee-ffff00002222", "  ", null, null)));

        HubNotionSyncService.SyncResult result = service.sync();

        ArgumentCaptor<HubLink> saved = ArgumentCaptor.forClass(HubLink.class);
        verify(links, times(1)).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("새 글");
        assertThat(saved.getValue().getUrl()).isEqualTo("https://app.notion.com/p/aaaabbbbccccddddeeeeffff00001111");
        assertThat(saved.getValue().getDescription()).isEqualTo("상태: 진행 중");
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).total()).isEqualTo(3);
    }

    @Test
    void doesNothingWithoutLinkedDatabases() {
        when(categories.findAll()).thenReturn(List.of(new HubCategory("문서", null)));
        when(links.findAllUrls()).thenReturn(List.of());

        assertThat(service.sync().added()).isZero();
        verify(notion, never()).queryDatabase(any());
    }
}
