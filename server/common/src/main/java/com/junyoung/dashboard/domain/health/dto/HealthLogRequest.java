package com.junyoung.dashboard.domain.health.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record HealthLogRequest(
        @NotNull LocalDateTime recordedAt,
        @Positive Double weightKg,
        @DecimalMin("0.0") @DecimalMax("24.0") Double sleepHours,
        @Size(max = 500) String notes
) {
}
