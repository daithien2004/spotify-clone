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
import com.spotify.auth.domain.event.UserRegistered;
import com.spotify.auth.domain.exception.UserAlreadyExistsException;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    public record Request(
            @NotBlank(message = "Email is required") @jakarta.validation.constraints.Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
            @NotBlank(message = "Display name is required") String displayName,
            String avatarUrl
    ) {}

    public record Response(String accessToken, String refreshToken, String userId, String email, String displayName, String avatarUrl) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public Response execute(Request request) {
        Email email = new Email(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(request.email());
        }

        Password password = new Password(request.password());
        String encodedPassword = passwordEncoderPort.encode(password.value());

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(new Password(encodedPassword))
                .displayName(request.displayName())
                .avatarUrl(request.avatarUrl())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        user = userRepository.save(user);
        domainEventPublisher.publish(new UserRegistered(user.getId(), user.getEmail().value()));

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
