package com.junyoung.dashboard.domain.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// reminder_raw 컬럼 길이(title_raw 200/target_at_raw 50/target_domain 50/sent_raw 10)를 넘으면
// DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고, 그 외 의미적 검증(날짜/불리언 파싱 등)은
// Kafka Consumer의 ETL 단계에서 수행한다.
public record RawReminderRequest(
        @NotBlank @Size(max = 200) String titleRaw,
        @NotBlank @Size(max = 50) String targetAtRaw,
        @Size(max = 50) String targetDomain,
        Long targetEntityId,
        @NotBlank @Size(max = 10) String sentRaw
) {
}
