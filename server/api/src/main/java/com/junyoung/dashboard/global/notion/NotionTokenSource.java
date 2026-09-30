package com.junyoung.dashboard.global.notion;

/**
 * 노션 API를 부를 때마다 쓸 토큰을 준다 (이슈 #227) — 웹 설정에서 토큰을 바꾸면 재시작 없이 다음 호출부터 반영된다.
 * 우선순위: 설정 화면에서 저장한 값(DB) → 환경변수 NOTION_TOKEN → 없음(null).
 */
public interface NotionTokenSource {
    String currentToken();
}
