package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HabitLogRequest(
        @NotNull Long habitId,
        @NotNull LocalDate performedAt,
        @NotNull Boolean completed,
        @Size(max = 500) String notes
) {
}
