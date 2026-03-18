package com.spotify.playlist.infrastructure.persistence.repository;

import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;

@Repository
public interface JpaPlaylistTrackRepository extends JpaRepository<PlaylistTrackJpaEntity, UUID> {
}
