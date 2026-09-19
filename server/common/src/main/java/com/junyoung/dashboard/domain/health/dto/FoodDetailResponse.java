package com.junyoung.dashboard.domain.health.dto;

import java.util.List;

public record FoodDetailResponse(String foodId, String name, String brandName, List<FoodServingResponse> servings) {
}
