package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.RawAssignment;

import java.time.LocalDateTime;

public record RawAssignmentResponse(
        Long id,
        String courseIdRaw,
        String titleRaw,
        String dueDateRaw,
        String completedRaw,
        String notes,
        String status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RawAssignmentResponse from(RawAssignment raw) {
        return new RawAssignmentResponse(
                raw.getId(),
                raw.getCourseIdRaw(),
                raw.getTitleRaw(),
                raw.getDueDateRaw(),
                raw.getCompletedRaw(),
                raw.getNotes(),
                raw.getStatus().name(),
                raw.getFailureReason(),
                raw.getCreatedAt(),
                raw.getUpdatedAt()
        );
    }
}
