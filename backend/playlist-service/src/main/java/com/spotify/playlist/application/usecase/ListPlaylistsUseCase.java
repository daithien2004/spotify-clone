package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistSummaryResponse;

import java.util.List;

public interface ListPlaylistsUseCase {
    List<PlaylistSummaryResponse> execute();
}