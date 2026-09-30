package com.junyoung.dashboard.global.notion;

/** 토큰으로 확인한 노션 통합(bot) — botId로 계정이 바뀌었는지 보고, workspaceName은 화면에 보여 준다 */
public record NotionAccount(String botId, String workspaceName) {
}
