package com.spotify.track.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.spotify.track.domain.entity.Track;

public interface TrackRepository {
    Track save(Track track);

    Optional<Track> findById(UUID id);

    /** Returns found tracks for the given ids; row order is not guaranteed. */
    List<Track> findAllByIds(List<UUID> ids);
}