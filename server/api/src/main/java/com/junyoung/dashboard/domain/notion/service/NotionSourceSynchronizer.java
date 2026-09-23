package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.hub.dto.RawHubLinkRequest;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.domain.hub.service.RawHubLinkService;
import com.junyoung.dashboard.domain.notion.NotionIds;
import com.junyoung.dashboard.domain.notion.client.NotionClient;
import com.junyoung.dashboard.domain.notion.client.NotionPage;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncResult;
import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncedPage;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncedPageRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// 소스 하나를 한 트랜잭션으로 동기화한다 — 한 소스가 실패해도 다른 소스의 결과는 커밋되도록 NotionSyncService와 빈을 나눴다.
@Component
public class NotionSourceSynchronizer {

    static final String LINK_DESCRIPTION = "노션 동기화";
    private static final int TITLE_MAX_LENGTH = 200;

    private final NotionClient notionClient;
    private final NotionSyncSourceRepository sourceRepository;
    private final NotionSyncedPageRepository syncedPageRepository;
    private final HubLinkRepository hubLinkRepository;
    private final RawHubLinkService rawHubLinkService;
    private final Clock clock;

    // 생성자가 둘이라 스프링이 사용할 생성자를 @Autowired로 명시한다(WeeklyStatJobRunner와 같은 이유).
    @Autowired
    public NotionSourceSynchronizer(NotionClient notionClient,
                                    NotionSyncSourceRepository sourceRepository,
                                    NotionSyncedPageRepository syncedPageRepository,
                                    HubLinkRepository hubLinkRepository,
                                    RawHubLinkService rawHubLinkService) {
        this(notionClient, sourceRepository, syncedPageRepository, hubLinkRepository, rawHubLinkService,
                Clock.systemDefaultZone());
    }

    NotionSourceSynchronizer(NotionClient notionClient,
                             NotionSyncSourceRepository sourceRepository,
                             NotionSyncedPageRepository syncedPageRepository,
                             HubLinkRepository hubLinkRepository,
                             RawHubLinkService rawHubLinkService,
                             Clock clock) {
        this.notionClient = notionClient;
        this.sourceRepository = sourceRepository;
        this.syncedPageRepository = syncedPageRepository;
        this.hubLinkRepository = hubLinkRepository;
        this.rawHubLinkService = rawHubLinkService;
        this.clock = clock;
    }

    @Transactional
    public NotionSyncResult sync(Long sourceId) {
        NotionSyncSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> EntityNotFoundException.of(NotionSyncSource.class, sourceId));
        Long categoryId = source.getHubCategory().getId();

        List<NotionPage> pages = source.getSourceType() == NotionSourceType.DATABASE
                ? notionClient.listDatabasePages(source.getNotionId())
                : notionClient.listChildPages(source.getNotionId());

        Set<String> alreadySynced = syncedPageRepository.findNotionPageIdsBySourceId(sourceId);
        // 카테고리에 이미 같은 노션 페이지를 가리키는 링크가 있으면(#94 이관분, 손으로 넣은 링크) 새로 만들지 않는다.
        Set<String> linkedInCategory = hubLinkRepository.findByCategoryId(categoryId).stream()
                .map(link -> NotionIds.extract(link.getUrl()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        int created = 0;
        int matched = 0;
        int skipped = 0;
        for (NotionPage page : pages) {
            if (alreadySynced.contains(page.id())) {
                continue;
            }
            // 제목 없는 페이지는 기록하지 않는다 — 나중에 제목이 생기면 다음 동기화에서 들어온다.
            if (page.title() == null || page.title().isBlank()) {
                skipped++;
                continue;
            }
            Long rawHubLinkId = null;
            if (linkedInCategory.contains(page.id())) {
                matched++;
            } else {
                rawHubLinkId = rawHubLinkService.create(new RawHubLinkRequest(
                        categoryId.toString(),
                        truncate(page.title().strip()),
                        NotionIds.pageUrl(page.id()),
                        LINK_DESCRIPTION
                )).id();
                created++;
            }
            syncedPageRepository.save(new NotionSyncedPage(source, page.id(), rawHubLinkId));
            alreadySynced.add(page.id());
        }
        source.markSynced(LocalDateTime.now(clock));
        return NotionSyncResult.success(sourceId, source.getNotionId(), created, matched, skipped);
    }

    private static String truncate(String title) {
        return title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title;
    }
}
