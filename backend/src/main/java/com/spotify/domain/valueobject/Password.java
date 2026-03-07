package com.spotify.domain.valueobject;

import com.spotify.domain.exception.DomainException;

public record Password(String value) {
    public Password {
        if (value == null || value.isBlank()) {
            throw new DomainException("Password cannot be null or empty");
        }
        if (value.length() < 6) {
            throw new DomainException("Password must be at least 6 characters long");
        }
    }
}
