package com.spotify.auth.infrastructure.persistence.user;

import com.spotify.auth.domain.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import com.spotify.auth.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity extends BaseJpaEntity {
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    // === Email Verification ===
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    // === Two-Factor Authentication ===
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2faEnabled = false;

    // === Account Lockout ===
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private java.time.OffsetDateTime lockedUntil;

}
