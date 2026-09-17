package com.junyoung.dashboard.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 의도적으로 형식/범위 검증이 없다 (필수 여부만 확인) — raw 입력이므로 실제 검증은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawEventRequest(
        @NotBlank @Size(max = 200) String titleRaw,
        @NotBlank @Size(max = 100) String startAtRaw,
        @NotBlank @Size(max = 100) String endAtRaw,
        @Size(max = 200) String location,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 20) String allDayRaw
) {
}
