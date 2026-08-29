package com.spotify.playlist.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class Playlist {
    private final UUID id;
    private final String title;
    private final String description;
    private final String ownerName;
    private final String coverUrl;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}