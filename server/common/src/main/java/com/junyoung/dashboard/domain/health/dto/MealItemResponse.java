package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealItem;

import java.time.LocalDateTime;

public record MealItemResponse(
        Long id,
        Long mealRecordId,
        String name,
        Integer calories,
        Double carbsG,
        Double proteinG,
        Double fatG,
        Double sodiumMg,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MealItemResponse from(MealItem item) {
        return new MealItemResponse(
                item.getId(),
                item.getMealRecord().getId(),
                item.getName(),
                item.getCalories(),
                item.getCarbsG(),
                item.getProteinG(),
                item.getFatG(),
                item.getSodiumMg(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
