package com.spotify.auth.infrastructure.persistence.token;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.spotify.auth.infrastructure.persistence.BaseJpaEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenJpaEntity extends BaseJpaEntity {
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "replaced_by")
    private String replacedBy;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

}
