package com.junyoung.dashboard.global.exception;

// Bean Validation으로 표현할 수 없는 요청 오류(중복 등록, 해석할 수 없는 값 등) — 400 INVALID_REQUEST로 응답한다.
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
