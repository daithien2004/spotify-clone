package com.spotify.auth.infrastructure.persistence.token;

import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository jpaRepository;
    private final RefreshTokenJpaMapper mapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = mapper.toJpaEntity(refreshToken);
        RefreshTokenJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        jpaRepository.revokeAllUserTokens(userId);
    }
}
