package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// hub_raw_link 컬럼 길이(category_id_raw 50/title_raw 200/url_raw 1000/description 500)를 넘으면
// DB 저장 자체가 실패하므로 그 상한만 방어적으로 유지하고, 그 외 의미적 검증(숫자 파싱, 부모 존재 확인 등)은
// Kafka Consumer의 ETL 단계에서 수행한다.
public record RawHubLinkRequest(
        @NotBlank @Size(max = 50) String categoryIdRaw,
        @NotBlank @Size(max = 200) String titleRaw,
        @NotBlank @Size(max = 1000) String urlRaw,
        @Size(max = 500) String description
) {
}
