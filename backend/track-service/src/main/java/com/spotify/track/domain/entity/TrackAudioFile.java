package com.spotify.track.domain.entity;

import java.io.InputStream;

/**
 * Audio payload returned by the storage port. InputStream is JDK-only —
 * keeps the domain layer free of Spring/MinIO imports.
 */
public record TrackAudioFile(
    InputStream content,
    long size,
    String contentType
) {
}