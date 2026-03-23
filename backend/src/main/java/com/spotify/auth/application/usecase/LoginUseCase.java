package com.spotify.auth.application.usecase;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    public record Request(
            @NotBlank(message = "Email is required") @jakarta.validation.constraints.Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") String password
    ) {}

    public record Response(String accessToken, String refreshToken, String userId, String email, String displayName, String avatarUrl) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;

    @Transactional
    public Response execute(Request request) {
        Email email = new Email(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Invalid email or password"));

        if (!passwordEncoderPort.matches(request.password(), user.getPassword().value())) {
            throw new DomainException("Invalid email or password");
        }

        String token = tokenPort.generateToken(user);
        String refreshTokenStr = tokenPort.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(refreshTokenStr)
                .userId(user.getId())
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(tokenPort.getRefreshTokenExpirationMillis())))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        return new Response(token, refreshTokenStr, user.getId().toString(), user.getEmail().value(),
                user.getDisplayName(), user.getAvatarUrl());
    }
}
