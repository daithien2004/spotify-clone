package com.spotify.track.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.repository.TrackRepository;
import com.spotify.track.infrastructure.persistence.mapper.TrackJpaMapper;
import com.spotify.track.infrastructure.persistence.repository.JpaTrackRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrackRepositoryImpl implements TrackRepository {

    private final JpaTrackRepository jpaRepository;
    private final TrackJpaMapper mapper;

    @Override
    public Track save(Track track) {
        // Save then re-map so the returned entity carries DB-generated createdAt/updatedAt
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(track)));
    }

    @Override
    public Optional<Track> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<Track> findAllByIds(List<UUID> ids) {
        // Callers (GetTrackByIdsUseCase) rebuild input order themselves
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<Track> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .toList();
    }
}