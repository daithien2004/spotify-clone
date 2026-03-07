package com.spotify.application.usecase.auth;

import com.spotify.application.dto.AuthResponse;
import com.spotify.application.dto.LoginRequest;
import com.spotify.application.port.out.PasswordEncoderPort;
import com.spotify.application.port.out.TokenPort;
import com.spotify.domain.entity.User;
import com.spotify.domain.exception.DomainException;
import com.spotify.domain.repository.UserRepository;
import com.spotify.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;

    @Transactional(readOnly = true)
    public AuthResponse execute(LoginRequest request) {
        Email email = new Email(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Invalid email or password"));

        if (!passwordEncoderPort.matches(request.password(), user.getPassword().value())) {
            throw new DomainException("Invalid email or password");
        }

        String token = tokenPort.generateToken(user);
        return new AuthResponse(token, user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl());
    }
}
