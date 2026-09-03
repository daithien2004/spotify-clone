package com.spotify.playlist.domain.repository;

import com.spotify.playlist.domain.entity.Playlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository {
    Optional<Playlist> findById(UUID id);

    List<Playlist> findAll();

    Playlist save(Playlist playlist);
}