package com.spotify.playlist.domain.entity;

import com.spotify.playlist.domain.valueobject.LexoRank;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PlaylistTrack {
    private final UUID id;
    private final UUID playlistId;
    private final UUID trackId;
    private LexoRank lexoRank;
    private final OffsetDateTime addedAt;
    private OffsetDateTime updatedAt;

    public void updateRank(LexoRank newRank) {
        this.lexoRank = newRank;
        this.updatedAt = OffsetDateTime.now();
    }
}
