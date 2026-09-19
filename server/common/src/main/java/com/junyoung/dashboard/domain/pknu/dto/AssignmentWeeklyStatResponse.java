package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.AssignmentWeeklyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssignmentWeeklyStatResponse(
        Long id,
        Long courseId,
        LocalDate weekStart,
        Integer totalCount,
        Integer completedCount,
        Double completionRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AssignmentWeeklyStatResponse from(AssignmentWeeklyStat stat) {
        // totalCount는 Reader가 마감 과제가 있는 코스만 고르므로 항상 1 이상이지만, 0 나눗셈은 방어한다.
        double rate = stat.getTotalCount() == 0 ? 0.0 : (double) stat.getCompletedCount() / stat.getTotalCount();
        return new AssignmentWeeklyStatResponse(
                stat.getId(),
                stat.getCourse().getId(),
                stat.getWeekStart(),
                stat.getTotalCount(),
                stat.getCompletedCount(),
                rate,
                stat.getCreatedAt(),
                stat.getUpdatedAt()
        );
    }
}
