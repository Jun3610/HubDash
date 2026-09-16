package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabitRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
