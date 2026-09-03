package com.spotify.user.infrastructure.persistence.repository;

import com.spotify.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    @Query("select u from UserJpaEntity u where u.id in :ids")
    List<UserJpaEntity> findAllByIdInOrder(@Param("ids") List<UUID> ids);
}
