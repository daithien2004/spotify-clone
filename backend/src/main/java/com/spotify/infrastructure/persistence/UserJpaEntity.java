package com.spotify.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserJpaEntity {
    @Id
    private UUID id;
    private String email;
    private String password;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
