package com.junyoung.dashboard.domain.pknu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotNull Long semesterId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String professor,
        @NotNull @Min(1) @Max(6) Integer credit,
        @Size(max = 1000) String notionUrl,
        // 4.5 만점 등급. 수정 요청에서 빠지면 null로 지워지므로 클라이언트는 항상 보낸다
        @Pattern(regexp = "A\\+|A0|B\\+|B0|C\\+|C0|D\\+|D0|F", message = "성적은 A+, A0, B+, B0, C+, C0, D+, D0, F 중 하나여야 합니다") String grade,
        @Size(max = 5000) String memo
) {
}
