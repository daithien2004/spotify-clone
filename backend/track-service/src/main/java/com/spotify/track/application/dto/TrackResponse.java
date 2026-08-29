package com.spotify.track.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.spotify.track.domain.entity.Track;

public record TrackResponse(
    UUID id,
    String title,
    String artist,
    String album,
    Long durationMs,
    String artworkUrl,
    String audioUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(
            track.getId(),
            track.getTitle(),
            track.getArtist(),
            track.getAlbum(),
            track.getDurationMs(),
            track.getArtworkUrl(),
            track.getAudioUrl(),
            track.getCreatedAt(),
            track.getUpdatedAt());
    }
}