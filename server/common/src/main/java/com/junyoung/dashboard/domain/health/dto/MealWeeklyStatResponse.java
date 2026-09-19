package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealWeeklyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MealWeeklyStatResponse(
        Long id,
        LocalDate weekStart,
        Integer dayCount,
        Double avgCalories,
        Double avgCarbsG,
        Double avgProteinG,
        Double avgFatG,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MealWeeklyStatResponse from(MealWeeklyStat stat) {
        return new MealWeeklyStatResponse(
                stat.getId(),
                stat.getWeekStart(),
                stat.getDayCount(),
                stat.getAvgCalories(),
                stat.getAvgCarbsG(),
                stat.getAvgProteinG(),
                stat.getAvgFatG(),
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
