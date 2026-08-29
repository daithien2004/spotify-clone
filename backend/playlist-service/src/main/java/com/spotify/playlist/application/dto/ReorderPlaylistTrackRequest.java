package com.spotify.playlist.application.dto;

import java.util.UUID;

public record ReorderPlaylistTrackRequest(
    UUID playlistId,
    UUID playlistTrackId,
    String prevRank,
    String nextRank
) {}
