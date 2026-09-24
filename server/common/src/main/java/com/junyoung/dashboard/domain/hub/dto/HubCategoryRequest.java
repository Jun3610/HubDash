package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HubCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        // 노션 DB 주소나 ID (선택). 서버가 32자리 ID만 뽑아 저장한다 (이슈 #163)
        @Size(max = 1000) String notionDatabase
) {
}
