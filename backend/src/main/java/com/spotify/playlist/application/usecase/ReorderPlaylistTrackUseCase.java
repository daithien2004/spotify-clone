package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.ReorderPlaylistTrackRequest;

public interface ReorderPlaylistTrackUseCase {
    void execute(ReorderPlaylistTrackRequest request);
}
