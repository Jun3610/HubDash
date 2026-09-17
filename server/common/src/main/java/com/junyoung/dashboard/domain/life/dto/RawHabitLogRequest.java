package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// life_raw_habit_log 컬럼 길이(habit_id_raw/performed_at_raw/completed_raw 50, notes 500)를 넘으면
// DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고, 그 외 의미적 검증(숫자/날짜/불리언 파싱,
// 부모 존재 확인 등)은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawHabitLogRequest(
        @NotBlank @Size(max = 50) String habitIdRaw,
        @NotBlank @Size(max = 50) String performedAtRaw,
        @NotBlank @Size(max = 50) String completedRaw,
        @Size(max = 500) String notes
) {
}
