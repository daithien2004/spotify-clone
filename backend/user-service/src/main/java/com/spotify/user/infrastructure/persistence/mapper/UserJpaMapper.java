package com.spotify.user.infrastructure.persistence.mapper;

import com.spotify.user.domain.entity.User;
import com.spotify.user.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserJpaMapper {

    @Mapping(target = "id", source = "id")
    UserJpaEntity toJpaEntity(User domain);

    User toDomainEntity(UserJpaEntity jpa);
}
