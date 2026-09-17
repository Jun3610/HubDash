package com.junyoung.dashboard.domain.study.dto;

import java.time.LocalDate;

public record StudyTopicWeeklyStatBatchRunResponse(Long jobExecutionId, String status, LocalDate weekStart) {
}
