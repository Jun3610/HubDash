package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudyProgressResponse(
        Long id,
        Long topicId,
        LocalDate studiedAt,
        Integer minutes,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyProgressResponse from(StudyProgress progress) {
        return new StudyProgressResponse(
                progress.getId(),
                progress.getTopic().getId(),
                progress.getStudiedAt(),
                progress.getMinutes(),
                progress.getNotes(),
                progress.getCreatedAt(),
                progress.getUpdatedAt()
        );
    }
}
