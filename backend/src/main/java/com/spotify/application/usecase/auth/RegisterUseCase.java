package com.spotify.application.usecase.auth;

import com.spotify.application.dto.AuthResponse;
import com.spotify.application.dto.RegisterRequest;
import com.spotify.application.port.out.PasswordEncoderPort;
import com.spotify.application.port.out.TokenPort;
import com.spotify.domain.entity.User;
import com.spotify.domain.exception.UserAlreadyExistsException;
import com.spotify.domain.repository.UserRepository;
import com.spotify.domain.valueobject.Email;
import com.spotify.domain.valueobject.Password;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;

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
                .createdAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        String token = tokenPort.generateToken(user);

        return new AuthResponse(token, user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl());
    }
}
