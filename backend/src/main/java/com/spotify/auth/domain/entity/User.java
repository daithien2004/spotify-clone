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

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private UUID id;
    private Email email;
    private Password password;
    private String displayName;
    private String avatarUrl;
    @Builder.Default
    private Set<Role> roles = Collections.singleton(Role.ROLE_USER);
    private OffsetDateTime createdAt;
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
