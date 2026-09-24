package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionIds;
import com.junyoung.dashboard.global.notion.NotionPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 'Notion 불러오기' (이슈 #163): 노션 DB가 연결된 허브 카테고리마다 DB 행을 읽어
 * 아직 링크로 없는 글만 새로 만든다. 이미 있는 링크는 수정·삭제하지 않는다 (사용자 결정).
 */
@Service
public class HubNotionSyncService {

    private final NotionClient notion;
    private final HubCategoryRepository categories;
    private final HubLinkRepository links;

    public HubNotionSyncService(NotionClient notion, HubCategoryRepository categories, HubLinkRepository links) {
        this.notion = notion;
        this.categories = categories;
        this.links = links;
    }

    public record CategoryResult(Long categoryId, String name, int added, int total) {
    }

    public record SyncResult(int added, List<CategoryResult> categories) {
    }

    @Transactional
    public SyncResult sync() {
        Set<String> known = new HashSet<>();
        for (String url : links.findAllUrls()) {
            String id = NotionIds.extract(url);
            if (id != null) {
                known.add(id);
            }
        }
        List<CategoryResult> results = new ArrayList<>();
        int added = 0;
        for (HubCategory c : categories.findAll()) {
            if (c.getNotionDatabaseId() == null) {
                continue;
            }
            List<NotionPage> pages = notion.queryDatabase(c.getNotionDatabaseId());
            int n = 0;
            for (NotionPage p : pages) {
                String id = NotionIds.extract(p.id());
                if (id == null || !known.add(id) || p.title().isBlank()) {
                    continue;
                }
                links.save(new HubLink(c, truncate(p.title(), 200), NotionIds.pageUrl(id), describe(p)));
                n++;
            }
            added += n;
            results.add(new CategoryResult(c.getId(), c.getName(), n, pages.size()));
        }
        return new SyncResult(added, results);
    }

    /** 이관 때와 같은 설명: "상태: 완료 · 마감일: 2026-06-03" */
    static String describe(NotionPage p) {
        List<String> parts = new ArrayList<>();
        if (p.status() != null) {
            parts.add("상태: " + p.status());
        }
        if (p.date() != null) {
            parts.add("마감일: " + p.date());
        }
        return parts.isEmpty() ? null : truncate(String.join(" · ", parts), 500);
    }

    private static String truncate(String s, int max) {
        return Objects.requireNonNull(s).length() <= max ? s : s.substring(0, max);
    }
}
