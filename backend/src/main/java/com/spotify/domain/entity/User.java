package com.spotify.domain.entity;

import com.spotify.domain.valueobject.Email;
import com.spotify.domain.valueobject.Password;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class User {
    private final UUID id;
    private final Email email;
    private Password password;
    private String displayName;
    private String avatarUrl;
    private final LocalDateTime createdAt;

    public void updatePassword(Password newPassword) {
        this.password = newPassword;
    }

    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }
}
