package com.junyoung.dashboard.domain.pknu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SemesterRequest(
        @NotNull @Size(max = 50) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
