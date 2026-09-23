package com.junyoung.dashboard.domain.notion.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notion_synced_page",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "notion_page_id"}))
public class NotionSyncedPage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private NotionSyncSource source;

    @Column(nullable = false, length = 32)
    private String notionPageId;

    // 비어 있으면 동기화가 새로 만든 링크가 아니라 카테고리에 원래 있던 링크와 짝지은 기록이다.
    private Long rawHubLinkId;

    public NotionSyncedPage(NotionSyncSource source, String notionPageId, Long rawHubLinkId) {
        this.source = source;
        this.notionPageId = notionPageId;
        this.rawHubLinkId = rawHubLinkId;
    }
}
