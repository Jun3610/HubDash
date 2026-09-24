package com.junyoung.dashboard.domain.schedule.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDateTime startAt,
        LocalDateTime endAt, // 선택 (이슈 #156)
        @Size(max = 200) String location,
        @Size(max = 1000) String description,
        @NotNull Boolean allDay
) {
    /** 끝나는 시각을 적었을 때만 시작보다 늦어야 한다 */
    @AssertTrue(message = "endAt는 startAt보다 이후여야 합니다")
    public boolean isEndAfterStart() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}
