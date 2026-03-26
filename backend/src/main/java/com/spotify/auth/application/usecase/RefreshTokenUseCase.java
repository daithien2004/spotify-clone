package com.spotify.auth.application.usecase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    @Schema(name = "RefreshTokenRequest")
    public record Request(
            @NotBlank(message = "Refresh token is required") String refreshToken,
            @Schema(hidden = true) String ipAddress,
            @Schema(hidden = true) String userAgent
    ) {}

    @Schema(name = "RefreshTokenResponse")
    public record Response(
            @JsonIgnore String accessToken,
            @JsonIgnore String refreshToken,
            String userId,
            String email,
            String displayName,
            String avatarUrl,
            long expiresIn
    ) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenPort tokenPort;
    private final SecurityAuditPublisher auditPublisher;

    @Transactional
    public Response execute(Request request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new DomainException("Invalid refresh token"));

        if (rt.isRevoked()) {
            List<RefreshToken> familyTokens = refreshTokenRepository.findAllByFamilyId(rt.getFamilyId());
            familyTokens.forEach(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });

            auditPublisher.publish(rt.getUserId().toString(), "unknown",
                    SecurityAuditPublisher.EventType.TOKEN_REUSE_DETECTED,
                    request.ipAddress(), request.userAgent(),
                    "Revoked refresh token reuse detected. Family ID: " + rt.getFamilyId() + ". All sessions revoked.");

            throw new DomainException("Security alert: Refresh token reuse detected. All sessions have been terminated.");
        }

        if (!rt.isValid()) {
            throw new DomainException("Refresh token is expired");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new DomainException("User not found"));

        String newRefreshTokenStr = tokenPort.generateRefreshToken();
        rt.markReplacedBy(newRefreshTokenStr);
        refreshTokenRepository.save(rt);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(newRefreshTokenStr)
                .userId(user.getId())
                .familyId(rt.getFamilyId())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(tokenPort.getRefreshTokenExpirationMillis())))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = tokenPort.generateToken(user);
        long expiresIn = tokenPort.getAccessTokenExpirationMillis() / 1000;

        return new Response(newAccessToken, newRefreshTokenStr, user.getId().toString(),
                user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl(), expiresIn);
    }
}
