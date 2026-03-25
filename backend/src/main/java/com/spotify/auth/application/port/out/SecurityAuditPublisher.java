package com.spotify.auth.application.port.out;

/**
 * Port để publish Security Audit Events ra Kafka.
 * Các service khác (email-service, monitoring) có thể lắng nghe và xử lý.
 */
public interface SecurityAuditPublisher {

    enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        EMAIL_VERIFIED,
        TOKEN_REUSE_DETECTED, // Nghi ngờ token bị đánh cắp
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        LOGOUT
    }

    void publish(String userId, String email, EventType eventType, String ipAddress, String userAgent, String detail);
}
