package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.HealthLogWeeklyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthLogWeeklyStatResponse(
        Long id,
        LocalDate weekStart,
        Integer logCount,
        Double avgWeightKg,
        Double avgSleepHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HealthLogWeeklyStatResponse from(HealthLogWeeklyStat stat) {
        return new HealthLogWeeklyStatResponse(
                stat.getId(),
                stat.getWeekStart(),
                stat.getLogCount(),
                stat.getAvgWeightKg(),
                stat.getAvgSleepHours(),
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
