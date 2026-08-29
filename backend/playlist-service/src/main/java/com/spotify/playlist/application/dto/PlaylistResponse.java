package com.spotify.playlist.application.dto;

import com.spotify.playlist.domain.entity.Playlist;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlaylistResponse(
    UUID id,
    String title,
    String description,
    String ownerName,
    String coverUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
            playlist.getId(),
            playlist.getTitle(),
            playlist.getDescription(),
            playlist.getOwnerName(),
            playlist.getCoverUrl(),
            playlist.getCreatedAt(),
            playlist.getUpdatedAt());
    }
}