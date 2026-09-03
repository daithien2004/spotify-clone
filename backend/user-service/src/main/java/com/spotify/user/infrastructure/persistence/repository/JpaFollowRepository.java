package com.spotify.user.infrastructure.persistence.repository;

import com.spotify.user.infrastructure.persistence.entity.FollowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFollowRepository extends JpaRepository<FollowJpaEntity, UUID> {

    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    List<FollowJpaEntity> findAllByFollowerId(UUID followerId);

    List<FollowJpaEntity> findAllByFolloweeId(UUID followeeId);
}
