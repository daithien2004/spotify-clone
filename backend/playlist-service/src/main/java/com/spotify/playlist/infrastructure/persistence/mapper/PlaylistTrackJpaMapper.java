package com.spotify.playlist.infrastructure.persistence.mapper;

import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.valueobject.LexoRank;
import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PlaylistTrackJpaMapper {

    @Mapping(target = "lexoRank", source = "lexoRank", qualifiedByName = "mapLexoRankToString")
    PlaylistTrackJpaEntity toJpaEntity(PlaylistTrack domain);

    @Mapping(target = "lexoRank", source = "lexoRank", qualifiedByName = "mapStringToLexoRank")
    PlaylistTrack toDomainEntity(PlaylistTrackJpaEntity jpa);

    @Named("mapLexoRankToString")
    default String mapLexoRankToString(LexoRank rank) {
        return rank != null ? rank.value() : null;
    }

    @Named("mapStringToLexoRank")
    default LexoRank mapStringToLexoRank(String value) {
        return value != null ? new LexoRank(value) : null;
    }
}
