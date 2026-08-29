package com.spotify.track.application.dto;

import java.io.InputStream;
import java.util.UUID;

public record UploadTrackAudioCommand(
    UUID trackId,
    InputStream content,
    long size,
    String contentType
) {
}