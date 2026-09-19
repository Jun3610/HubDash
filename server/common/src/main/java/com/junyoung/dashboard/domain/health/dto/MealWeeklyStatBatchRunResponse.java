package com.junyoung.dashboard.domain.health.dto;

import java.time.LocalDate;

public record MealWeeklyStatBatchRunResponse(Long jobExecutionId, String status, LocalDate weekStart) {
}
