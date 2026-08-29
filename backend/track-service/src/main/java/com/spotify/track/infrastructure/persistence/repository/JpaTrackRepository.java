package com.spotify.track.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spotify.track.infrastructure.persistence.entity.TrackJpaEntity;

@Repository
public interface JpaTrackRepository extends JpaRepository<TrackJpaEntity, UUID> {
    // findAllById(Iterable<UUID>) inherited — row order is not guaranteed
}