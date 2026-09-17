package com.junyoung.dashboard.domain.schedule.dto;

import com.junyoung.dashboard.domain.schedule.entity.RawEvent;

import java.time.LocalDateTime;

public record RawEventResponse(
        Long id,
        String titleRaw,
        String startAtRaw,
        String endAtRaw,
        String location,
        String description,
        String allDayRaw,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawEventResponse from(RawEvent raw) {
        return new RawEventResponse(
                raw.getId(),
                raw.getTitleRaw(),
                raw.getStartAtRaw(),
                raw.getEndAtRaw(),
                raw.getLocation(),
                raw.getDescription(),
                raw.getAllDayRaw(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
