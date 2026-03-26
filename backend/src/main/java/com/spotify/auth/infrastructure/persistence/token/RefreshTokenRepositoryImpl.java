package com.spotify.auth.infrastructure.persistence.token;

import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository jpaRepository;
    private final RefreshTokenJpaMapper mapper;
    private final EntityManager entityManager;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = mapper.toJpaEntity(refreshToken);
        
        entity.setToken(hashToken(refreshToken.getToken()));
        if (refreshToken.getReplacedBy() != null) {
            entity.setReplacedBy(hashToken(refreshToken.getReplacedBy()));
        }
        
        entity = entityManager.merge(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(hashToken(token))
                .map(mapper::toDomain);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        jpaRepository.revokeAllUserTokens(userId);
    }

    @Override
    public List<RefreshToken> findAllByFamilyId(UUID familyId) {
        return jpaRepository.findByFamilyId(familyId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
