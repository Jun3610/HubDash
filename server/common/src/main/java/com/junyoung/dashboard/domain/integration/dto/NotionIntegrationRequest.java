package com.junyoung.dashboard.domain.integration.dto;

import jakarta.validation.constraints.Size;

/** token이 비어 있으면 저장된 토큰을 그대로 쓴다. scheduleDatabase는 노션 DB 주소나 ID */
public record NotionIntegrationRequest(
        @Size(max = 500) String token,
        @Size(max = 500) String scheduleDatabase
) {
    @Override
    public String toString() {
        return "NotionIntegrationRequest(scheduleDatabase=" + scheduleDatabase + ")";
    }
}
