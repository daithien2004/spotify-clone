package com.spotify.auth.domain.event;

import java.util.UUID;

public class UserRegistered extends DomainEvent {
    private final UUID userId;
    private final String email;
    private final String displayName;

    public UserRegistered(UUID userId, String email, String displayName) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
