package com.spotify.user.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User projection trong user_db — nguồn từ Kafka User.Registered (auth-service).
 * Không phải credentials/identity source; chỉ chứa dữ liệu profile dùng cho follows.
 */
@Getter
@Builder
public class User {
    private final UUID id;
    private final String email;
    private String displayName;
    private String avatarUrl;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.updatedAt = OffsetDateTime.now();
    }
}
