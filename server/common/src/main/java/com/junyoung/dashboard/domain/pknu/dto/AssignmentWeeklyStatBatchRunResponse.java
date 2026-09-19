package com.junyoung.dashboard.domain.pknu.dto;

import java.time.LocalDate;

public record AssignmentWeeklyStatBatchRunResponse(Long jobExecutionId, String status, LocalDate weekStart) {
}
