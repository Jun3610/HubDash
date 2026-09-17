package com.junyoung.dashboard.domain.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// study_raw_progress 컬럼 길이(topic_id_raw/studied_at_raw/minutes_raw 50, notes 1000)를 넘으면
// DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고, 그 외 의미적 검증(숫자/날짜 파싱, 부모 존재 확인,
// 범위 검증 등)은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawStudyProgressRequest(
        @NotBlank @Size(max = 50) String topicIdRaw,
        @NotBlank @Size(max = 50) String studiedAtRaw,
        @NotBlank @Size(max = 50) String minutesRaw,
        @Size(max = 1000) String notes
) {
}
