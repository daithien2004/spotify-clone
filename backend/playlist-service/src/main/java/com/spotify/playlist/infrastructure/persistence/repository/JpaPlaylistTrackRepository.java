package com.spotify.playlist.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;

@Repository
public interface JpaPlaylistTrackRepository extends JpaRepository<PlaylistTrackJpaEntity, UUID> {

    List<PlaylistTrackJpaEntity> findByPlaylistIdOrderByLexoRankAsc(UUID playlistId);

    @Query("select distinct pt.playlistId from PlaylistTrackJpaEntity pt where length(pt.lexoRank) > :minLength")
    List<UUID> findPlaylistIdsNeedingRebalance(@Param("minLength") int minLength);
}
