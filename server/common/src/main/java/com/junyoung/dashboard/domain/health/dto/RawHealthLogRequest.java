package com.junyoung.dashboard.domain.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 의도적으로 형식/범위 검증이 없다 (recordedAtRaw 필수 여부만 확인) — raw 입력이므로 실제 검증은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawHealthLogRequest(
        @NotBlank @Size(max = 100) String recordedAtRaw,
        @Size(max = 50) String weightKgRaw,
        Double sleepHoursRaw,
        @Size(max = 500) String notes
) {
}
