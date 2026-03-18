package com.spotify.playlist.infrastructure.persistence.adapter;

import org.apache.el.stream.Optional;
import org.hibernate.validator.constraints.UUID;
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
    public void save(PlaylistTrack playlistTrack) {
        jpaRepository.save(mapper.toJpaEntity(playlistTrack));
    }
}
