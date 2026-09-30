package com.junyoung.dashboard.domain.integration.service;

/** 노션 연동의 비밀이 아닌 설정 (config_json) */
public record NotionSettings(String botId, String workspaceName, String scheduleDatabaseId) {
    static final NotionSettings EMPTY = new NotionSettings(null, null, null);
}
