package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;

import java.time.LocalDateTime;

public record HubCategoryResponse(
        Long id,
        String name,
        String description,
        String notionDatabaseId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubCategoryResponse from(HubCategory category) {
        return new HubCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getNotionDatabaseId(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
