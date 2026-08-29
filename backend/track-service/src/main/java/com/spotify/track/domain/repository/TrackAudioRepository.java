package com.spotify.track.domain.repository;

import com.spotify.track.domain.entity.TrackAudioRange;

import java.io.InputStream;
import java.util.UUID;

/** Storage port for raw audio — MinIO is the production adapter. */
public interface TrackAudioRepository {
    void putAudio(UUID trackId, InputStream content, long size, String contentType);

    /** Loads byte range [offset, offset+length) of the track's audio object. */
    TrackAudioRange getAudio(UUID trackId, long offset, long length);

    void deleteAudio(UUID trackId);
}