package com.junyoung.dashboard.domain.integration.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/** 비어 있는 값은 저장된 값을 그대로 쓴다. calendars는 가져올 캘린더 이름 (null이면 유지, 빈 목록이면 모두 해제) */
public record ICloudIntegrationRequest(
        @Size(max = 320) String appleId,
        @Size(max = 100) String appPassword,
        @Size(max = 50) List<@Size(max = 200) String> calendars
) {
    @Override
    public String toString() {
        return "ICloudIntegrationRequest(appleId=" + appleId + ", calendars=" + calendars + ")";
    }
}
