package com.spotify.auth.domain.exception;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String email) {
        super("User with email " + email + " already exists");
    }
}
