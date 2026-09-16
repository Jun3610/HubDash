package com.junyoung.dashboard.domain.study.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudyProgressRequest(
        @NotNull Long topicId,
        @NotNull LocalDate studiedAt,
        @NotNull Integer minutes,
        @Size(max = 1000) String notes
) {
}
