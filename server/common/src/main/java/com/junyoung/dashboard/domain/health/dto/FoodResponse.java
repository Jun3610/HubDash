package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.Food;

import java.time.LocalDateTime;

public record FoodResponse(
        Long id,
        String name,
        Integer calories,
        Double carbsG,
        Double fatG,
        Double proteinG,
        String serving,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FoodResponse from(Food food) {
        return new FoodResponse(
                food.getId(),
                food.getName(),
                food.getCalories(),
                food.getCarbsG(),
                food.getFatG(),
                food.getProteinG(),
                food.getServing(),
                food.isPinned(),
                food.getCreatedAt(),
                food.getUpdatedAt()
        );
    }
}
