package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistTrackResponse;

import java.util.List;
import java.util.UUID;

public interface GetPlaylistTracksUseCase {
    List<PlaylistTrackResponse> execute(UUID playlistId);
}