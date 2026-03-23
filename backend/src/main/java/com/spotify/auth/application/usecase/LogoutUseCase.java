package com.spotify.auth.application.usecase;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotBlank;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.infrastructure.security.JwtBlacklistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    public record Request(
            @NotBlank(message = "Refresh token is required") String refreshToken
    ) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtBlacklistService jwtBlacklistService;
    private final TokenPort tokenPort;

    @Transactional
    public void execute(Request request, String accessToken) {
        // Revoke the refresh token
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(rt -> {
                    rt.revoke();
                    refreshTokenRepository.save(rt);
                });

        // Blacklist the access token
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String token = accessToken.substring(7);
            try {
                Date expiration = tokenPort.getExpirationFromToken(token);
                long timeLeft = expiration.getTime() - System.currentTimeMillis();
                jwtBlacklistService.blacklistToken(token, timeLeft);
            } catch (Exception e) {
                // Token might already be expired or invalid, skipping blacklist safely
            }
        }
    }
}
