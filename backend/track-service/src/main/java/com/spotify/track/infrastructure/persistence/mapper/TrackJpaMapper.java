package com.spotify.track.infrastructure.persistence.mapper;

import com.spotify.track.domain.entity.Track;
import com.spotify.track.infrastructure.persistence.entity.TrackJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrackJpaMapper {

    TrackJpaEntity toJpaEntity(Track domain);

    Track toDomainEntity(TrackJpaEntity jpa);
}