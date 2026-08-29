package com.spotify.playlist.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddTrackToPlaylistRequest(
    UUID playlistId,
    @NotNull UUID trackId
) {}