package com.junyoung.dashboard.domain.health.dto;

public record FoodSearchItemResponse(
        String foodId,
        String name,
        String brandName,
        String type,
        String description
) {
}
