package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.WorkoutLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkoutLogResponse(
        Long id,
        LocalDate performedAt,
        String type,
        Integer durationMinutes,
        Integer caloriesBurned,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkoutLogResponse from(WorkoutLog log) {
        return new WorkoutLogResponse(
                log.getId(),
                log.getPerformedAt(),
                log.getType(),
                log.getDurationMinutes(),
                log.getCaloriesBurned(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
