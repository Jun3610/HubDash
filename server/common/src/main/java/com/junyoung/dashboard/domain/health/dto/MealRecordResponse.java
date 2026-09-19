package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;

import java.time.LocalDateTime;
import java.util.List;

public record MealRecordResponse(
        Long id,
        LocalDateTime consumedAt,
        MealType mealType,
        String notes,
        List<MealItemResponse> items,
        MealTotals totals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MealRecordResponse from(MealRecord record) {
        return new MealRecordResponse(
                record.getId(),
                record.getConsumedAt(),
                record.getMealType(),
                record.getNotes(),
                record.getItems().stream().map(MealItemResponse::from).toList(),
                MealTotals.of(record.getItems()),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
