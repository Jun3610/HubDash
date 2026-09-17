package com.junyoung.dashboard.domain.pknu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// pknu_raw_assignment 컬럼 길이(course_id_raw/due_date_raw/completed_raw 50, title_raw 200, notes 500)를
// 넘으면 DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고, 그 외 의미적 검증(숫자/날짜/불리언 파싱,
// 부모 존재 확인 등)은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawAssignmentRequest(
        @NotBlank @Size(max = 50) String courseIdRaw,
        @NotBlank @Size(max = 200) String titleRaw,
        @NotBlank @Size(max = 50) String dueDateRaw,
        @NotBlank @Size(max = 50) String completedRaw,
        @Size(max = 500) String notes
) {
}
