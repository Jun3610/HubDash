package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;

import java.time.LocalDateTime;

public record MealRecordResponse(
        Long id,
        LocalDateTime consumedAt,
        MealType mealType,
        Integer calories,
        Double carbsG,
        Double proteinG,
        Double fatG,
        Double sodiumMg,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MealRecordResponse from(MealRecord record) {
        return new MealRecordResponse(
                record.getId(),
                record.getConsumedAt(),
                record.getMealType(),
                record.getCalories(),
                record.getCarbsG(),
                record.getProteinG(),
                record.getFatG(),
                record.getSodiumMg(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
