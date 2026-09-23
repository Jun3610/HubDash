package com.junyoung.dashboard.domain.notion.client;

// 동기화에 필요한 것만 — 페이지 ID(하이픈 없는 32자리 소문자)와 제목.
public record NotionPage(String id, String title) {
}
