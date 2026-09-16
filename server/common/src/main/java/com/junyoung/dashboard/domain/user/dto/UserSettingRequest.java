package com.junyoung.dashboard.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserSettingRequest(
        @NotBlank @Size(max = 20) String theme,
        @NotBlank @Size(max = 10) String language,
        @NotNull Boolean notificationEnabled
) {
}
