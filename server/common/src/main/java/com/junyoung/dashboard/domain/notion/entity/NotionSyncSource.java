package com.junyoung.dashboard.domain.notion.entity;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notion_sync_source")
public class NotionSyncSource extends BaseEntity {

    // 하이픈 없는 32자리 소문자 노션 ID로 저장한다 — 같은 DB/페이지가 다른 표기로 두 번 등록되지 않게.
    @Column(nullable = false, length = 32, unique = true)
    private String notionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotionSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_category_id", nullable = false)
    private HubCategory hubCategory;

    private LocalDateTime lastSyncedAt;

    public NotionSyncSource(String notionId, NotionSourceType sourceType, HubCategory hubCategory) {
        this.notionId = notionId;
        this.sourceType = sourceType;
        this.hubCategory = hubCategory;
    }

    public void markSynced(LocalDateTime syncedAt) {
        this.lastSyncedAt = syncedAt;
    }
}
