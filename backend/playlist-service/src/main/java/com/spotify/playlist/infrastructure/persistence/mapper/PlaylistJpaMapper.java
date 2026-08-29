package com.spotify.playlist.infrastructure.persistence.mapper;

import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.infrastructure.persistence.entity.PlaylistJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlaylistJpaMapper {
    PlaylistJpaEntity toJpaEntity(Playlist domain);

    Playlist toDomainEntity(PlaylistJpaEntity jpa);
}