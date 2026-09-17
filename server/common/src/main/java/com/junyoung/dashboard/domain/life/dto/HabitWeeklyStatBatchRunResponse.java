package com.junyoung.dashboard.domain.life.dto;

import java.time.LocalDate;

public record HabitWeeklyStatBatchRunResponse(Long jobExecutionId, String status, LocalDate weekStart) {
}
