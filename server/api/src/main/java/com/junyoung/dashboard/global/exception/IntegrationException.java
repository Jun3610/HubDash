package com.junyoung.dashboard.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 연동(노션, iCloud) 설정·검증 오류 — 사용자에게 그대로 보여 줄 문구를 담는다 (이슈 #227).
 * 메시지에 비밀값(토큰, 앱 암호)을 넣지 않는다.
 */
public class IntegrationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public IntegrationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
