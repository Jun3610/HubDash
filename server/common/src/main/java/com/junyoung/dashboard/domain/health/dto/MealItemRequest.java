package com.junyoung.dashboard.domain.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MealItemRequest(
        @NotNull Long mealRecordId,
        @NotBlank @Size(max = 100) String name,
        @NotNull @PositiveOrZero Integer calories,
        @PositiveOrZero Double carbsG,
        @PositiveOrZero Double proteinG,
        @PositiveOrZero Double fatG,
        @PositiveOrZero Double sodiumMg
) {
}
