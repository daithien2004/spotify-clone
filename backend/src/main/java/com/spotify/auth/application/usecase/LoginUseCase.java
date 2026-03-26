package com.spotify.auth.application.usecase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    @Schema(name = "LoginRequest")
    public record Request(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") String password,
            @Schema(hidden = true) String ipAddress,
            @Schema(hidden = true) String userAgent
    ) {}

    @Schema(name = "LoginResponse")
    public record Response(
            @JsonIgnore String accessToken,
            @JsonIgnore String refreshToken,
            String userId,
            String email,
            String displayName,
            String avatarUrl,
            long expiresIn
    ) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;
    private final SecurityAuditPublisher auditPublisher;

    @Transactional
    public Response execute(Request request) {
        com.spotify.auth.domain.valueobject.Email email = new com.spotify.auth.domain.valueobject.Email(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Invalid email or password"));

        if (user.isLocked()) {
            auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
                    SecurityAuditPublisher.EventType.LOGIN_FAILED, request.ipAddress(), request.userAgent(),
                    "Account is locked due to too many failed attempts");
            throw new DomainException("Account is temporarily locked. Please try again later.");
        }

        if (!passwordEncoderPort.matches(request.password(), user.getPassword().value())) {
            user.recordFailedLogin();
            userRepository.save(user);

            auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
                    SecurityAuditPublisher.EventType.LOGIN_FAILED, request.ipAddress(), request.userAgent(),
                    "Wrong password attempt #" + user.getFailedLoginAttempts());

            if (user.isLocked()) {
                auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
                        SecurityAuditPublisher.EventType.ACCOUNT_LOCKED, request.ipAddress(), request.userAgent(),
                        "Account locked after " + User.MAX_FAILED_ATTEMPTS + " failed attempts");
                throw new DomainException("Too many failed attempts. Account locked for " + User.LOCKOUT_DURATION_MINUTES + " minutes.");
            }

            throw new DomainException("Invalid email or password");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        String accessToken = tokenPort.generateToken(user);
        String refreshTokenStr = tokenPort.generateRefreshToken();
        UUID familyId = UUID.randomUUID();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(refreshTokenStr)
                .userId(user.getId())
                .familyId(familyId)
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(tokenPort.getRefreshTokenExpirationMillis())))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
                SecurityAuditPublisher.EventType.LOGIN_SUCCESS, request.ipAddress(), request.userAgent(), null);

        long expiresIn = tokenPort.getAccessTokenExpirationMillis() / 1000;

        return new Response(accessToken, refreshTokenStr, user.getId().toString(),
                user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl(), expiresIn);
    }
}
