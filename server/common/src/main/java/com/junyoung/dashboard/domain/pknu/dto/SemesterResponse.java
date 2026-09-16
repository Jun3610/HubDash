package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Semester;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SemesterResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SemesterResponse from(Semester semester) {
        return new SemesterResponse(
                semester.getId(),
                semester.getName(),
                semester.getStartDate(),
                semester.getEndDate(),
                semester.getCreatedAt(),
                semester.getUpdatedAt()
        );
    }
}
