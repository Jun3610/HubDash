package com.junyoung.dashboard.domain.memo.dto;

import com.junyoung.dashboard.domain.memo.entity.Memo;

import java.time.LocalDateTime;

public record MemoResponse(
        Long id,
        String title,
        String content,
        String tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getTags(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
