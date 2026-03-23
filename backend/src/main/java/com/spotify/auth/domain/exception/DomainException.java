package com.spotify.auth.domain.exception;

public class DomainException extends RuntimeException {
    private final int statusCode;

    public DomainException(String message) {
        super(message);
        this.statusCode = 400; // Default mapping to BAD_REQUEST (400)
    }

    public DomainException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
