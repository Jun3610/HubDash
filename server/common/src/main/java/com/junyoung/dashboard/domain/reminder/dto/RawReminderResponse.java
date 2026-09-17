package com.junyoung.dashboard.domain.reminder.dto;

import com.junyoung.dashboard.domain.reminder.entity.RawReminder;

import java.time.LocalDateTime;

public record RawReminderResponse(
        Long id,
        String titleRaw,
        String targetAtRaw,
        String targetDomain,
        Long targetEntityId,
        String sentRaw,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawReminderResponse from(RawReminder raw) {
        return new RawReminderResponse(
                raw.getId(),
                raw.getTitleRaw(),
                raw.getTargetAtRaw(),
                raw.getTargetDomain(),
                raw.getTargetEntityId(),
                raw.getSentRaw(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
