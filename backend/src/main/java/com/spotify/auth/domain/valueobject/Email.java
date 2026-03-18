package com.spotify.auth.domain.valueobject;

import com.spotify.auth.domain.exception.DomainException;

public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainException("Email cannot be null or empty");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new DomainException("Invalid email format");
        }
    }
}
