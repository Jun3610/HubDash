package com.junyoung.dashboard.domain.pknu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignmentRequest(
        @NotNull Long courseId,
        @NotNull @Size(max = 200) String title,
        @NotNull LocalDate dueDate,
        @NotNull Boolean completed,
        @Size(max = 500) String notes
) {
}
