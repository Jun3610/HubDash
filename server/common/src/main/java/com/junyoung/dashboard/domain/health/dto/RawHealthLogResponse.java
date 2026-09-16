package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.RawHealthLog;

import java.time.LocalDateTime;

public record RawHealthLogResponse(
        Long id,
        String recordedAtRaw,
        String weightKgRaw,
        Double sleepHoursRaw,
        String notes,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawHealthLogResponse from(RawHealthLog raw) {
        return new RawHealthLogResponse(
                raw.getId(),
                raw.getRecordedAtRaw(),
                raw.getWeightKgRaw(),
                raw.getSleepHoursRaw(),
                raw.getNotes(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
