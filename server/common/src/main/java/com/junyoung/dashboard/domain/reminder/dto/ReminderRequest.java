package com.junyoung.dashboard.domain.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReminderRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDateTime targetAt,
        @Size(max = 50) String targetDomain,
        Long targetEntityId,
        @NotNull Boolean sent
) {
}
