package com.junyoung.dashboard.domain.schedule.dto;

import com.junyoung.dashboard.domain.schedule.entity.Event;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        String description,
        Boolean allDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartAt(),
                event.getEndAt(),
                event.getLocation(),
                event.getDescription(),
                event.getAllDay(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
