package com.spotify.auth.application.port.out;

public interface SecurityAuditPublisher {
    enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        EMAIL_VERIFIED,
        TOKEN_REUSE_DETECTED,
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        LOGOUT
    }

    void publish(String userId, String email, EventType eventType, String ipAddress, String userAgent, String detail);
}
