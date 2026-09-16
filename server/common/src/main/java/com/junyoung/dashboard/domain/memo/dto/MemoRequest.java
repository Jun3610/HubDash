package com.junyoung.dashboard.domain.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 300) String tags
) {
}
