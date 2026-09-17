package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.RawHubLink;

import java.time.LocalDateTime;

public record RawHubLinkResponse(
        Long id,
        String categoryIdRaw,
        String titleRaw,
        String urlRaw,
        String description,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawHubLinkResponse from(RawHubLink raw) {
        return new RawHubLinkResponse(
                raw.getId(),
                raw.getCategoryIdRaw(),
                raw.getTitleRaw(),
                raw.getUrlRaw(),
                raw.getDescription(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
