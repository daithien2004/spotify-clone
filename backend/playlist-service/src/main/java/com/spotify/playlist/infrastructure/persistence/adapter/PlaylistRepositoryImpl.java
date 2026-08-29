package com.spotify.playlist.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import com.spotify.playlist.infrastructure.persistence.mapper.PlaylistJpaMapper;
import com.spotify.playlist.infrastructure.persistence.repository.JpaPlaylistRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements PlaylistRepository {

    private final JpaPlaylistRepository jpaRepository;
    private final PlaylistJpaMapper mapper;

    @Override
    public Optional<Playlist> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<Playlist> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomainEntity).toList();
    }
}