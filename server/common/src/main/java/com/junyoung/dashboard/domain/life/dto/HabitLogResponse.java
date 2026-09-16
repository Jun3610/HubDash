package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.HabitLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitLogResponse(
        Long id,
        Long habitId,
        LocalDate performedAt,
        Boolean completed,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HabitLogResponse from(HabitLog log) {
        return new HabitLogResponse(
                log.getId(),
                log.getHabit().getId(),
                log.getPerformedAt(),
                log.getCompleted(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
