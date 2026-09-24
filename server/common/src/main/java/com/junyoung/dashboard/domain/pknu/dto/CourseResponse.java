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
        String grade,
        String memo,
        String tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 태그 이전 응답 모양 (테스트 호환) */
    public CourseResponse(Long id, Long semesterId, String name, String professor, Integer credit, String notionUrl,
                          String grade, String memo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, semesterId, name, professor, credit, notionUrl, grade, memo, null, createdAt, updatedAt);
    }

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getSemester().getId(),
                course.getName(),
                course.getProfessor(),
                course.getCredit(),
                course.getNotionUrl(),
                course.getGrade(),
                course.getMemo(),
                course.getTags(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
