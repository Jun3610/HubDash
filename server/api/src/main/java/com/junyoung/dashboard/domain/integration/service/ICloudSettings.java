package com.junyoung.dashboard.domain.integration.service;

import java.util.List;

/** iCloud 연동의 비밀이 아닌 설정 (config_json) — 가져올 캘린더 이름 */
public record ICloudSettings(String appleId, List<String> calendars) {
    static final ICloudSettings EMPTY = new ICloudSettings(null, List.of());

    public ICloudSettings {
        calendars = calendars == null ? List.of() : List.copyOf(calendars);
    }
}
