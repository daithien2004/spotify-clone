package com.spotify.track.domain.exception;

import java.util.UUID;

/** Track not found — a client error, mapped to 404 by the track GlobalExceptionHandler. */
public class TrackNotFoundException extends RuntimeException {
    public TrackNotFoundException(UUID trackId) {
        super("Track not found: " + trackId);
    }
}