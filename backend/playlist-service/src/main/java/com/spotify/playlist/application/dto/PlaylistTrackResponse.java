package com.spotify.playlist.application.dto;

import com.spotify.playlist.domain.entity.PlaylistTrack;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlaylistTrackResponse(
    UUID id,
    UUID trackId,
    String lexoRank,
    OffsetDateTime addedAt,
    OffsetDateTime updatedAt
) {
    public static PlaylistTrackResponse from(PlaylistTrack track) {
        return new PlaylistTrackResponse(
            track.getId(),
            track.getTrackId(),
            track.getLexoRank().value(),
            track.getAddedAt(),
            track.getUpdatedAt());
    }
}