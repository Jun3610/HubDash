package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Course;

import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        Long semesterId,
        String name,
        String professor,
        Integer credit,
        String notionUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getSemester().getId(),
                course.getName(),
                course.getProfessor(),
                course.getCredit(),
                course.getNotionUrl(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
