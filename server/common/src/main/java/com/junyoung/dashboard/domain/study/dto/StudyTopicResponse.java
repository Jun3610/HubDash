package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;

import java.time.LocalDateTime;

public record StudyTopicResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyTopicResponse from(StudyTopic topic) {
        return new StudyTopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}
