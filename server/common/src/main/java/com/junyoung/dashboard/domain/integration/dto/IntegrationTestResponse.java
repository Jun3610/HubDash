package com.junyoung.dashboard.domain.integration.dto;

import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;

import java.time.LocalDateTime;

/** 연결 확인 결과 — 실패해도 200으로 돌려주고 ok=false + error로 알린다 */
public record IntegrationTestResponse(
        IntegrationProvider provider,
        boolean ok,
        String account,
        String error,
        LocalDateTime checkedAt
) {
}
