package com.spotify.auth.infrastructure.persistence.token;

import com.spotify.auth.domain.entity.RefreshToken;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenJpaMapper {
    RefreshToken toDomain(RefreshTokenJpaEntity entity);
    RefreshTokenJpaEntity toJpaEntity(RefreshToken domain);
}
