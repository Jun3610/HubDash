package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReadingLogResponse(
        Long id,
        String title,
        String author,
        LocalDate startedAt,
        LocalDate finishedAt,
        Integer rating,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReadingLogResponse from(ReadingLog log) {
        return new ReadingLogResponse(
                log.getId(),
                log.getTitle(),
                log.getAuthor(),
                log.getStartedAt(),
                log.getFinishedAt(),
                log.getRating(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
