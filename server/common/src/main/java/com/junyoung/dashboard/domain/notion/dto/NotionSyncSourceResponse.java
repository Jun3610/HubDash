package com.junyoung.dashboard.domain.notion.dto;

import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;

import java.time.LocalDateTime;

public record NotionSyncSourceResponse(
        Long id,
        String notionId,
        NotionSourceType sourceType,
        Long hubCategoryId,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotionSyncSourceResponse from(NotionSyncSource source) {
        return new NotionSyncSourceResponse(
                source.getId(),
                source.getNotionId(),
                source.getSourceType(),
                source.getHubCategory().getId(),
                source.getLastSyncedAt(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
