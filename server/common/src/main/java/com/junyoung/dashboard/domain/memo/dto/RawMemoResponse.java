package com.junyoung.dashboard.domain.memo.dto;

import com.junyoung.dashboard.domain.memo.entity.RawMemo;

import java.time.LocalDateTime;

public record RawMemoResponse(
        Long id,
        String titleRaw,
        String contentRaw,
        String tags,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawMemoResponse from(RawMemo raw) {
        return new RawMemoResponse(
                raw.getId(),
                raw.getTitleRaw(),
                raw.getContentRaw(),
                raw.getTags(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
