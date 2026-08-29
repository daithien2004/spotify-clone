package com.spotify.track.domain.event;

import java.util.UUID;

/** Track.AudioUploaded — emitted when an audio file lands in storage (domain.md event map). */
public class TrackAudioUploaded extends DomainEvent {
    private final UUID trackId;

    public TrackAudioUploaded(UUID trackId) {
        this.trackId = trackId;
    }

    public UUID getTrackId() {
        return trackId;
    }
}