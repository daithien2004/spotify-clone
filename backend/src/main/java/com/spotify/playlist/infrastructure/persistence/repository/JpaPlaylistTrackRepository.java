package com.spotify.playlist.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;

@Repository
public interface JpaPlaylistTrackRepository extends JpaRepository<PlaylistTrackJpaEntity, UUID> {
}
