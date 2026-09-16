package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssignmentResponse(
        Long id,
        Long courseId,
        String title,
        LocalDate dueDate,
        Boolean completed,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getTitle(),
                assignment.getDueDate(),
                assignment.getCompleted(),
                assignment.getNotes(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
