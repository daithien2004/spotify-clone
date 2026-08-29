package com.spotify.auth.domain.repository;

import com.spotify.auth.domain.entity.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllUserTokens(UUID userId);

    /** Lấy tất cả tokens trong cùng một Token Family (dùng cho Reuse Detection) */
    List<RefreshToken> findAllByFamilyId(UUID familyId);
}
