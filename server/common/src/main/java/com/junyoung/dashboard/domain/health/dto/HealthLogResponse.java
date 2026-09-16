package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.HealthLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthLogResponse(
        Long id,
        LocalDate recordedAt,
        Double weightKg,
        Double sleepHours,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HealthLogResponse from(HealthLog log) {
        return new HealthLogResponse(
                log.getId(),
                log.getRecordedAt(),
                log.getWeightKg(),
                log.getSleepHours(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
