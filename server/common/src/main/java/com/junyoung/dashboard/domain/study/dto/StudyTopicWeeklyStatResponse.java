package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyTopicWeeklyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudyTopicWeeklyStatResponse(
        Long id,
        Long topicId,
        LocalDate weekStart,
        Integer sessionCount,
        Integer totalMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyTopicWeeklyStatResponse from(StudyTopicWeeklyStat stat) {
        return new StudyTopicWeeklyStatResponse(
                stat.getId(),
                stat.getTopic().getId(),
                stat.getWeekStart(),
                stat.getSessionCount(),
                stat.getTotalMinutes(),
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
