package com.junyoung.dashboard.domain.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 의도적으로 길이 상한 없는 형식 검증만 최소로 둔다 (필수 여부만 확인) — raw 입력이므로 실제 검증은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawMemoRequest(
        @NotBlank String titleRaw,
        @NotBlank String contentRaw,
        @Size(max = 300) String tags
) {
}
