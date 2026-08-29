package com.spotify.playlist.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.spotify.playlist.domain.entity.PlaylistTrack;

public interface PlaylistTrackRepository {
    Optional<PlaylistTrack> findById(UUID id);

    void save(PlaylistTrack playlistTrack);
}
