package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReadingLogRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 100) String author,
        @NotNull LocalDate startedAt,
        LocalDate finishedAt,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String notes
) {
}
