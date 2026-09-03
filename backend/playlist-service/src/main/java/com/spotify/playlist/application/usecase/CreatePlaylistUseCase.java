package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.usecase.CreatePlaylistUseCase.CreatePlaylistCommand;
import com.spotify.playlist.application.dto.PlaylistResponse;

import java.util.UUID;

public interface CreatePlaylistUseCase {

    record CreatePlaylistCommand(UUID ownerUserId, String title, String description) {
    }

    PlaylistResponse execute(CreatePlaylistCommand command);
}
