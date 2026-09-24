package com.junyoung.dashboard.domain.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FoodRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @PositiveOrZero Integer calories,
        @PositiveOrZero Double carbsG,
        @PositiveOrZero Double fatG,
        @PositiveOrZero Double proteinG,
        @Size(max = 50) String serving,
        Boolean pinned
) {
    /** 고정 여부를 빠뜨리면 고정 안 함 */
    public boolean pinnedOrFalse() {
        return Boolean.TRUE.equals(pinned);
    }
}
