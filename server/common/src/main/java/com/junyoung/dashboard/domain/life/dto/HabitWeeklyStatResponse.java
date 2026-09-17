package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.HabitWeeklyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitWeeklyStatResponse(
        Long id,
        Long habitId,
        LocalDate weekStart,
        Integer totalCount,
        Integer completedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HabitWeeklyStatResponse from(HabitWeeklyStat stat) {
        return new HabitWeeklyStatResponse(
                stat.getId(),
                stat.getHabit().getId(),
                stat.getWeekStart(),
                stat.getTotalCount(),
                stat.getCompletedCount(),
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
