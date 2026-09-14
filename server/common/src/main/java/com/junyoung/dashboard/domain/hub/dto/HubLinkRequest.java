package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HubLinkRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 1000) String url,
        @Size(max = 500) String description
) {
}
