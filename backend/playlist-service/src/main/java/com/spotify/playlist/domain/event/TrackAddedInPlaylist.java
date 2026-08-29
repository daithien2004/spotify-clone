package com.spotify.playlist.domain.event;

import java.util.UUID;

public class TrackAddedInPlaylist extends DomainEvent {
    private final UUID playlistId;
    private final UUID trackId;

    public TrackAddedInPlaylist(UUID playlistId, UUID trackId) {
        this.playlistId = playlistId;
        this.trackId = trackId;
    }

    public UUID getPlaylistId() {
        return playlistId;
    }

    public UUID getTrackId() {
        return trackId;
    }
}