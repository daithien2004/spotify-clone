package com.spotify.playlist.application.usecase;

import java.util.UUID;

public interface RebalancePlaylistUseCase {
    void execute(UUID playlistId);
}
