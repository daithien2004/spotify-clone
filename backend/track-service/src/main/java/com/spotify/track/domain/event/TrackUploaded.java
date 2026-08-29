package com.spotify.track.domain.event;

import java.util.UUID;

/** Track.Uploaded — emitted when a track enters the catalog (domain.md event map). */
public class TrackUploaded extends DomainEvent {
    private final UUID trackId;
    private final String title;
    private final String artist;

    public TrackUploaded(UUID trackId, String title, String artist) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}