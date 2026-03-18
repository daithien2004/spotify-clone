package com.spotify.auth.application.usecase;

import com.spotify.auth.application.dto.AuthResponse;
import com.spotify.auth.application.dto.RegisterRequest;
import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.UserAlreadyExistsException;
import com.spotify.auth.domain.event.UserRegistered;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
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

        return new AuthResponse(token, user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl());
    }
}
