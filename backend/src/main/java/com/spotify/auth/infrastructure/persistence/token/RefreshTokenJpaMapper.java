package com.spotify.auth.infrastructure.persistence.token;

import com.spotify.auth.domain.entity.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefreshTokenJpaMapper {
    RefreshToken toDomain(RefreshTokenJpaEntity entity);

    @Mapping(target = "_isNew", ignore = true)
    RefreshTokenJpaEntity toJpaEntity(RefreshToken domain);
}
