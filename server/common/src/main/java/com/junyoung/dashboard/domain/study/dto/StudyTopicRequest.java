package com.junyoung.dashboard.domain.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyTopicRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 1000) String notionUrl
) {
}
