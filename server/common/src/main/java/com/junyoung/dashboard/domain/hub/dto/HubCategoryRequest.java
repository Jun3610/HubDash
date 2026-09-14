package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HubCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
