package com.spotify.track.application.dto;

import java.util.UUID;

public record GetTrackAudioCommand(
    UUID trackId,
    long offset,
    long length
) {
}