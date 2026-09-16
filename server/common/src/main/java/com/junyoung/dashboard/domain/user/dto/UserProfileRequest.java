package com.junyoung.dashboard.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 200) String email,
        @Size(max = 500) String bio
) {
}
