package com.spotify.track.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Inbound contract for create (POST) and full-update (PUT) — PUT is a full metadata snapshot. */
public record CreateTrackRequest(
    @NotBlank(message = "title is required") String title,
    @NotBlank(message = "artist is required") String artist,
    String album,
    @NotNull(message = "durationMs is required") @PositiveOrZero(message = "durationMs must be >= 0") Long durationMs,
    String artworkUrl
) {
}