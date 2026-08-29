package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.AddTrackToPlaylistRequest;
import com.spotify.playlist.domain.entity.PlaylistTrack;

public interface AddTrackToPlaylistUseCase {
    PlaylistTrack execute(AddTrackToPlaylistRequest request);
}