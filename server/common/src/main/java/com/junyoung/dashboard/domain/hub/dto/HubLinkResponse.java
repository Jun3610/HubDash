package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubLink;

import java.time.LocalDateTime;

public record HubLinkResponse(
        Long id,
        Long categoryId,
        String title,
        String url,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubLinkResponse from(HubLink link) {
        return new HubLinkResponse(
                link.getId(),
                link.getCategory().getId(),
                link.getTitle(),
                link.getUrl(),
                link.getDescription(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
