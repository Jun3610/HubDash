package com.junyoung.dashboard.domain.pknu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotNull Long semesterId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String professor,
        @NotNull @Min(1) @Max(6) Integer credit,
        @Size(max = 1000) String notionUrl
) {
}
