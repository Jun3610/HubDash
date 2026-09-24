package com.junyoung.dashboard.global.notion;

/** 노션 API를 쓸 수 없을 때 (토큰 없음, 권한 없음, 노션 오류) — 사용자에게 그대로 보여 줄 문구를 담는다 */
public class NotionException extends RuntimeException {
    public NotionException(String message) {
        super(message);
    }
}
