package com.junyoung.dashboard.domain.health.dto;

import java.util.List;

public record FoodSearchResponse(List<FoodSearchItemResponse> items, int page, int size, long totalResults) {
}
