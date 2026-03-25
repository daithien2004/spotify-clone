package com.spotify.auth.domain.entity;

import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.Collections;
import java.util.UUID;

// Số lần đăng nhập thất bại tối đa trước khi khoá tài khoản (Brute Force protection)
// Thời gian khoá tài khoản: 15 phút

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    // === Core Identity ===
    private UUID id;
    private Email email;
    private Password password;
    private String displayName;
    private String avatarUrl;
    @Builder.Default
    private Set<Role> roles = Collections.singleton(Role.ROLE_USER);

    // === Email Verification ===
    @Builder.Default
    private boolean isVerified = false;

    // === Two-Factor Authentication (TOTP) ===
    private String totpSecret;     // Base32 secret cho Google Authenticator
    @Builder.Default
    private boolean twoFaEnabled = false;  // Đổi tên để tránh Lombok tạo isIs2faEnabled()

    // === Account Lockout (Brute Force Protection) ===
    @Builder.Default
    private int failedLoginAttempts = 0;
    private OffsetDateTime lockedUntil;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    // =====================================================================
    // Domain Behaviors
    // =====================================================================

    public void updatePassword(Password newPassword) {
        this.password = newPassword;
        this.failedLoginAttempts = 0; // Reset failed attempts khi đổi mật khẩu
        this.lockedUntil = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Xác thực email thành công */
    public void verifyEmail() {
        this.isVerified = true;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Kiểm tra tài khoản có đang bị khoá không */
    public boolean isLocked() {
        if (lockedUntil == null) return false;
        if (OffsetDateTime.now().isAfter(lockedUntil)) {
            // Hết thời gian khoá — tự động mở khoá
            this.lockedUntil = null;
            this.failedLoginAttempts = 0;
            return false;
        }
        return true;
    }

    /** Ghi nhận đăng nhập thất bại, khoá tài khoản nếu vượt ngưỡng */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = OffsetDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
        }
        this.updatedAt = OffsetDateTime.now();
    }

    /** Reset trạng thái đăng nhập sau khi thành công */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Bật 2FA sau khi user xác nhận TOTP code đúng */
    public void enable2fa(String totpSecret) {
        this.totpSecret = totpSecret;
        this.twoFaEnabled = true;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Tắt 2FA */
    public void disable2fa() {
        this.totpSecret = null;
        this.twoFaEnabled = false;
        this.updatedAt = OffsetDateTime.now();
    }
}
