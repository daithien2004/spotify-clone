package com.spotify.playlist.domain.event;

import java.util.UUID;

public class TrackMovedInPlaylist extends DomainEvent {
    private final UUID playlistId;
    private final UUID trackId;
    private final String newRank;

    public TrackMovedInPlaylist(UUID playlistId, UUID trackId, String newRank) {
        this.playlistId = playlistId;
        this.trackId = trackId;
        this.newRank = newRank;
    }

    public UUID getPlaylistId() {
        return playlistId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public String getNewRank() {
        return newRank;
    }
}
