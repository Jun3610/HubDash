package com.junyoung.dashboard.domain.health.external.fatsecret;

public class FatSecretException extends RuntimeException {

    public enum Reason {
        NOT_CONFIGURED,
        AUTH_FAILED,
        IP_NOT_ALLOWED,
        RATE_LIMITED,
        NOT_FOUND,
        INVALID_REQUEST,
        UPSTREAM_ERROR
    }

    private final Reason reason;

    public FatSecretException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public FatSecretException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
