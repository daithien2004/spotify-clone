package com.spotify.track.domain.entity;

import java.io.InputStream;

/**
 * Audio byte range returned by the storage port. InputStream is JDK-only —
 * keeps the domain layer free of Spring/MinIO imports. For non-range
 * requests offset=0 and length=totalSize.
 */
public record TrackAudioRange(
    InputStream content,
    long offset,
    long length,
    long totalSize,
    String contentType
) {
}