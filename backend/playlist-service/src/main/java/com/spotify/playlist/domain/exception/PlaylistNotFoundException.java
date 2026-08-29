package com.spotify.playlist.domain.exception;

import java.util.UUID;

public class PlaylistNotFoundException extends RuntimeException {

    public PlaylistNotFoundException(UUID playlistId) {
        super("Playlist not found: " + playlistId);
    }
}