package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;

import java.time.LocalDateTime;

public record HabitResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HabitResponse from(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getDescription(),
                habit.getCreatedAt(),
                habit.getUpdatedAt()
        );
    }
}
