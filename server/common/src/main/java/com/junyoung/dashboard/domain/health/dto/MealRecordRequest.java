package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MealRecordRequest(
        @NotNull LocalDateTime consumedAt,
        @NotNull MealType mealType,
        @NotNull @PositiveOrZero Integer calories,
        @PositiveOrZero Double carbsG,
        @PositiveOrZero Double proteinG,
        @PositiveOrZero Double fatG,
        @PositiveOrZero Double sodiumMg,
        @Size(max = 500) String notes
) {
}
