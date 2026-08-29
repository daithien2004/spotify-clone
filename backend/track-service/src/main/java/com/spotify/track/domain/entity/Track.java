package com.spotify.track.domain.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Track {
    private final UUID id;
    private final String title;
    private final String artist;
    private final String album;
    private final Long durationMs;
    private final String artworkUrl;
    private final String audioUrl;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    /** Immutable-style metadata update — new entity, audioUrl carried over untouched (MinIO fills it later). */
    public Track withUpdatedMetadata(String title, String artist, String album, Long durationMs, String artworkUrl) {
        return Track.builder()
                .id(this.id)
                .title(title)
                .artist(artist)
                .album(album)
                .durationMs(durationMs)
                .artworkUrl(artworkUrl)
                .audioUrl(this.audioUrl)
                .createdAt(this.createdAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /** Immutable-style audio link — metadata carried over, only stream path changes. */
    public Track withAudioUrl(String audioUrl) {
        return Track.builder()
                .id(this.id)
                .title(this.title)
                .artist(this.artist)
                .album(this.album)
                .durationMs(this.durationMs)
                .artworkUrl(this.artworkUrl)
                .audioUrl(audioUrl)
                .createdAt(this.createdAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}