package com.junyoung.dashboard.domain.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// raw_memo 컬럼 길이(title_raw 200/content_raw 5000)를 넘으면 DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고,
// 그 외 의미적 검증(비어있지 않은지 자체는 여기서 걸러도 되지만 세부 규칙)은 Kafka Consumer의 ETL 단계에서 수행한다.
public record RawMemoRequest(
        @NotBlank @Size(max = 200) String titleRaw,
        @NotBlank @Size(max = 5000) String contentRaw,
        @Size(max = 300) String tags
) {
}
