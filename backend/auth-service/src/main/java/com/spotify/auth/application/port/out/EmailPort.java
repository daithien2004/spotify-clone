package com.spotify.auth.application.port.out;

/**
 * Port để gửi email giao dịch (transactional emails).
 * Infrastructure adapter sẽ dùng Spring Mail + Thymeleaf template.
 */
public interface EmailPort {

    /** Gửi email xác thực tài khoản với link chứa verification token */
    void sendVerificationEmail(String toEmail, String displayName, String verificationLink);

    /** Gửi email reset mật khẩu với link chứa reset token */
    void sendPasswordResetEmail(String toEmail, String displayName, String resetLink);

    /** Gửi thông báo đăng nhập từ thiết bị lạ */
    void sendNewLoginNotification(String toEmail, String displayName, String ipAddress, String userAgent);
}
