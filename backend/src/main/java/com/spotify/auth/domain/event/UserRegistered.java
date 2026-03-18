package com.spotify.auth.domain.event;

import java.util.UUID;

public class UserRegistered extends DomainEvent {
    private final UUID userId;
    private final String email;

    public UserRegistered(UUID userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
