package com.junyoung.dashboard.global.notion;

/** 노션 DB 한 행에서 허브 링크로 쓰는 값만 */
public record NotionPage(String id, String title, String status, String date) {
}
