package com.spotify.track.domain.exception;

import java.util.UUID;

public class TrackAudioNotFoundException extends RuntimeException {

    public TrackAudioNotFoundException(UUID trackId) {
        super("Audio not found for track: " + trackId);
    }
}