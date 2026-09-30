package com.junyoung.dashboard.domain.integration.dto;

/** 저장 결과 — 계정이 바뀌었으면 이전 계정에서 가져온 일정은 다음 동기화에서 지워진다고 알려 줄 수 있게 */
public record IntegrationSaveResponse(IntegrationResponse integration, boolean accountChanged) {
}
