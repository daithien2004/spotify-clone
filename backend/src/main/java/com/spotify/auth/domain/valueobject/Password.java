package com.spotify.auth.domain.valueobject;

import com.spotify.auth.domain.exception.DomainException;

public record Password(String value) {
    public Password {
        if (value == null || value.isBlank()) {
            throw new DomainException("Password cannot be null or empty");
        }
        if (value.length() < 8) {
            throw new DomainException("Password must be at least 8 characters");
        }
        if (!value.matches(".*[a-zA-Z].*") || !value.matches(".*\\d.*")) {
            throw new DomainException("Password must contain at least one letter and one number");
        }
    }
}
