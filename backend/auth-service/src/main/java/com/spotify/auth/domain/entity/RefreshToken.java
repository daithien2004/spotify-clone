package com.spotify.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh Token với Token Family pattern:
 * - familyId: nhóm tất cả token trong cùng một phiên đăng nhập.
 * - Nếu một token cũ trong family được dùng lại (reuse), toàn bộ family bị revoke
 *   => Force logout tất cả thiết bị. Đây là chuẩn bảo mật OAuth2 RFC 6749.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private UUID id;
    private String token;
    private UUID userId;
    private UUID familyId;      // Token Family ID — dùng để detect reuse attack
    private String replacedBy;  // Token mới thay thế token này (sau rotation)
    private String ipAddress;   // IP của thiết bị khi đăng nhập
    private String userAgent;   // Browser / App name
    private OffsetDateTime expiresAt;
    @Builder.Default
    private boolean revoked = false;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(OffsetDateTime.now());
    }

    /** Đánh dấu token này đã bị thay thế bởi token mới (Token Rotation) */
    public void markReplacedBy(String newToken) {
        this.replacedBy = newToken;
        this.revoked = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void revoke() {
        this.revoked = true;
        this.updatedAt = OffsetDateTime.now();
    }
}
