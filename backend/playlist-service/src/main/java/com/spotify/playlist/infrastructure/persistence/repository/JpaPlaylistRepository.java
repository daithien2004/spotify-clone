package com.spotify.playlist.infrastructure.persistence.repository;

import com.spotify.playlist.infrastructure.persistence.entity.PlaylistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPlaylistRepository extends JpaRepository<PlaylistJpaEntity, UUID> {
}