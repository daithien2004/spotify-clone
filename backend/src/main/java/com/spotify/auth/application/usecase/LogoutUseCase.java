package com.spotify.auth.application.usecase;

import io.swagger.v3.oas.annotations.media.Schema;

import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCase {

    @Schema(name = "LogoutRequest")
    public record Request(
            @NotBlank(message = "Refresh token is required") String refreshToken
    ) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final TokenPort tokenPort;

    @Transactional
    public void execute(Request request, String accessToken) {
        // 1. Revoke the refresh token in DB
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(rt -> {
                    rt.revoke();
                    refreshTokenRepository.save(rt);
                    log.debug("Refresh token revoked for user");
                });

        // 2. Blacklist the access token in Redis
        try {
            if (accessToken != null && !accessToken.isBlank()) {
                String tokenHash = DigestUtils.sha256Hex(accessToken);
                Date expiration = tokenPort.getExpirationFromToken(accessToken);
                long ttlMillis = expiration.getTime() - System.currentTimeMillis();

                if (ttlMillis > 0) {
                    redisTemplate.opsForValue().set(
                            "blacklist:" + tokenHash,
                            "true",
                            Duration.ofMillis(ttlMillis)
                    );
                    log.info("Access token blacklisted successfully");
                }
            }
        } catch (Exception e) {
            log.error("Failed to blacklist access token: {}", e.getClass().getSimpleName());
            // We don't fail the whole logout if blacklist fails, but we log it.
        }
    }
}
