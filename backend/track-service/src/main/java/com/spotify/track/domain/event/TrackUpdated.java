package com.spotify.track.domain.event;

import java.util.UUID;

/** Track.Updated — emitted when catalog metadata changes (domain.md event map). */
public class TrackUpdated extends DomainEvent {
    private final UUID trackId;
    private final String title;
    private final String artist;
    private final String album;
    private final Long durationMs;
    private final String artworkUrl;
    private final String audioUrl;

    public TrackUpdated(UUID trackId, String title, String artist, String album,
                        Long durationMs, String artworkUrl, String audioUrl) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
        this.artworkUrl = artworkUrl;
        this.audioUrl = audioUrl;
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

    public String getAlbum() {
        return album;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}