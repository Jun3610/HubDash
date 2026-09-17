package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.RawStudyProgress;

import java.time.LocalDateTime;

public record RawStudyProgressResponse(
        Long id,
        String topicIdRaw,
        String studiedAtRaw,
        String minutesRaw,
        String notes,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawStudyProgressResponse from(RawStudyProgress raw) {
        return new RawStudyProgressResponse(
                raw.getId(),
                raw.getTopicIdRaw(),
                raw.getStudiedAtRaw(),
                raw.getMinutesRaw(),
                raw.getNotes(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
