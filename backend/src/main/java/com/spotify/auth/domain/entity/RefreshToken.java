package com.spotify.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private UUID id;
    private String token;
    private UUID userId;
    private OffsetDateTime expiresAt;
    @Builder.Default
    private boolean revoked = false;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void revoke() {
        this.revoked = true;
        this.updatedAt = OffsetDateTime.now();
    }
}
