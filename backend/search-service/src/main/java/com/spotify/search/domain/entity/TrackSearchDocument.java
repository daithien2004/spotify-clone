package com.spotify.search.domain.entity;

import java.util.UUID;

/** Searchable snapshot of a track — mirrors the ES index `tracks` mapping (spec §5). */
public record TrackSearchDocument(
        UUID id,
        String title,
        String artist,
        String album,
        Long durationMs,
        String artworkUrl,
        String audioUrl
) {}
