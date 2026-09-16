package com.junyoung.dashboard.domain.reminder.dto;

import com.junyoung.dashboard.domain.reminder.entity.Reminder;

import java.time.LocalDateTime;

public record ReminderResponse(
        Long id,
        String title,
        LocalDateTime targetAt,
        String targetDomain,
        Long targetEntityId,
        Boolean sent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getTargetAt(),
                reminder.getTargetDomain(),
                reminder.getTargetEntityId(),
                reminder.getSent(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
