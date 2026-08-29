package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistResponse;

import java.util.UUID;

public interface GetPlaylistByIdUseCase {
    PlaylistResponse execute(UUID playlistId);
}