package com.junyoung.dashboard.domain.health.dto;

import java.time.LocalDate;

public record HealthLogWeeklyStatBatchRunResponse(Long jobExecutionId, String status, LocalDate weekStart) {
}
