package com.spotify.playlist.domain.repository;

import org.apache.el.stream.Optional;
import org.hibernate.validator.constraints.UUID;

import com.spotify.playlist.domain.entity.PlaylistTrack;

public interface PlaylistTrackRepository {
    Optional<PlaylistTrack> findById(UUID id);

    void save(PlaylistTrack playlistTrack);
}
