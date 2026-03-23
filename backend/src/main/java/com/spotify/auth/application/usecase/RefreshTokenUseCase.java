package com.spotify.auth.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.NotBlank;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    public record Request(
            @NotBlank(message = "Refresh token is required") String refreshToken
    ) {}

    public record Response(String accessToken, String refreshToken, String userId, String email, String displayName, String avatarUrl) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenPort tokenPort;

    @Transactional(readOnly = true)
    public Response execute(Request request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new DomainException("Invalid refresh token"));

        if (!rt.isValid()) {
            throw new DomainException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new DomainException("User not found"));

        String newAccessToken = tokenPort.generateToken(user);

        return new Response(newAccessToken, rt.getToken(), user.getId().toString(), user.getEmail().value(),
                user.getDisplayName(), user.getAvatarUrl());
    }
}
