package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.MealType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// 끼니 자체(언제, 어떤 끼니)만 받는다. 음식은 MealItem API로 추가한다.
public record MealRecordRequest(
        @NotNull LocalDateTime consumedAt,
        @NotNull MealType mealType,
        @Size(max = 500) String notes
) {
}
