package com.junyoung.dashboard.domain.pknu.dto;

import java.time.LocalDate;

// weekStart 생략 시 서비스가 "지난주 월요일"을 기본값으로 계산한다.
public record AssignmentWeeklyStatBatchRunRequest(LocalDate weekStart) {
}
