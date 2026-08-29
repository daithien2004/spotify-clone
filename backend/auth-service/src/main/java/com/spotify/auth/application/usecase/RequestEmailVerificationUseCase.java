package com.spotify.auth.application.usecase;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.EmailPort;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UseCase: Yêu cầu gửi lại email xác thực tài khoản.
 * Token được lưu trong Redis với TTL 24h.
 */
@Service
@RequiredArgsConstructor
public class RequestEmailVerificationUseCase {

    private static final long TTL_24H_SECONDS = 24 * 60 * 60L;
    private static final String TOKEN_TYPE = "EMAIL_VERIFICATION";

    @Schema(name = "RequestEmailVerificationRequest")
    public record Request(
        @NotBlank @Email String email
    ) {}

    private final UserRepository userRepository;
    private final SecurityTokenPort securityTokenPort;
    private final EmailPort emailPort;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Transactional
    public void execute(Request request) {
        com.spotify.auth.domain.valueobject.Email email = new com.spotify.auth.domain.valueobject.Email(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("User not found"));

        if (user.isVerified()) {
            throw new DomainException("Email is already verified");
        }

        String token = UUID.randomUUID().toString();
        securityTokenPort.save(token, user.getId(), TOKEN_TYPE, TTL_24H_SECONDS);

        String verificationLink = baseUrl + "/verify-email?token=" + token;
        emailPort.sendVerificationEmail(user.getEmail().value(), user.getDisplayName(), verificationLink);
    }
}
