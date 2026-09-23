package com.junyoung.dashboard.domain.notion.dto;

import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// notion은 노션 ID(하이픈 유무 무관) 또는 노션 페이지/DB 주소를 받는다.
public record NotionSyncSourceRequest(
        @NotBlank @Size(max = 500) String notion,
        @NotNull NotionSourceType sourceType,
        @NotNull Long hubCategoryId
) {
}
