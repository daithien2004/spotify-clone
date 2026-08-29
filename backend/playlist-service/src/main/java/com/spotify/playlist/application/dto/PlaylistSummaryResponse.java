package com.spotify.playlist.application.dto;

import com.spotify.playlist.domain.entity.Playlist;

import java.util.UUID;

/** Lightweight list entry for the library sidebar — no timestamps needed. */
public record PlaylistSummaryResponse(
    UUID id,
    String title,
    String ownerName,
    String coverUrl
) {
    public static PlaylistSummaryResponse from(Playlist playlist) {
        return new PlaylistSummaryResponse(
            playlist.getId(),
            playlist.getTitle(),
            playlist.getOwnerName(),
            playlist.getCoverUrl());
    }
}