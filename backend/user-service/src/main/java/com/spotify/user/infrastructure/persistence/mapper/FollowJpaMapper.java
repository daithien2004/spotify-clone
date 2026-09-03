package com.spotify.user.infrastructure.persistence.mapper;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.infrastructure.persistence.entity.FollowJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowJpaMapper {

    FollowJpaEntity toJpaEntity(Follow domain);

    Follow toDomainEntity(FollowJpaEntity jpa);
}
