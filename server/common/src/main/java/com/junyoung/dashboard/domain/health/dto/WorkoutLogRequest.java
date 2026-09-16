package com.junyoung.dashboard.domain.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WorkoutLogRequest(
        @NotNull LocalDate performedAt,
        @NotBlank @Size(max = 100) String type,
        @NotNull @Positive Integer durationMinutes,
        @PositiveOrZero Integer caloriesBurned,
        @Size(max = 500) String notes
) {
}
