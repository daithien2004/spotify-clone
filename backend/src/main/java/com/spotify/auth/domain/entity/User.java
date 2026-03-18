package com.spotify.auth.domain.entity;

import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class User {
    private final UUID id;
    private final Email email;
    private Password password;
    private String displayName;
    private String avatarUrl;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void updatePassword(Password newPassword) {
        this.password = newPassword;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.updatedAt = OffsetDateTime.now();
    }
}
