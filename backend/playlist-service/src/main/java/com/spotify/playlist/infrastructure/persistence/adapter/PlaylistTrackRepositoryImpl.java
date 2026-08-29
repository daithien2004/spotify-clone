package com.spotify.playlist.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.infrastructure.persistence.mapper.PlaylistTrackJpaMapper;
import com.spotify.playlist.infrastructure.persistence.repository.JpaPlaylistTrackRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistTrackRepositoryImpl implements PlaylistTrackRepository {

    private final JpaPlaylistTrackRepository jpaRepository;
    private final PlaylistTrackJpaMapper mapper;

    @Override
    public Optional<PlaylistTrack> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<PlaylistTrack> findAllByPlaylistId(UUID playlistId) {
        // Derived query orders by lexo_rank ascending — callers rely on this order
        return jpaRepository.findByPlaylistIdOrderByLexoRankAsc(playlistId).stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<UUID> findPlaylistIdsNeedingRebalance(int minRankLength) {
        return jpaRepository.findPlaylistIdsNeedingRebalance(minRankLength);
    }

    @Override
    public void save(PlaylistTrack playlistTrack) {
        jpaRepository.save(mapper.toJpaEntity(playlistTrack));
    }

    @Override
    public void saveAll(List<PlaylistTrack> playlistTracks) {
        jpaRepository.saveAll(playlistTracks.stream().map(mapper::toJpaEntity).toList());
    }
}