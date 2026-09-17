package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.RawHabitLog;

import java.time.LocalDateTime;

public record RawHabitLogResponse(
        Long id,
        String habitIdRaw,
        String performedAtRaw,
        String completedRaw,
        String notes,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawHabitLogResponse from(RawHabitLog raw) {
        return new RawHabitLogResponse(
                raw.getId(),
                raw.getHabitIdRaw(),
                raw.getPerformedAtRaw(),
                raw.getCompletedRaw(),
                raw.getNotes(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
