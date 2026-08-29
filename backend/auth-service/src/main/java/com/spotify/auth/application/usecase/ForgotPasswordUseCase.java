package com.spotify.auth.application.usecase;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.EmailPort;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UseCase: Gửi email link reset mật khẩu.
 * Token dùng 1 lần, lưu Redis TTL 15 phút.
 * Luôn trả về thành công dù email có tồn tại hay không (chống enumeration attack).
 */
@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private static final long TTL_15MIN_SECONDS = 15 * 60L;
    private static final String TOKEN_TYPE = "PASSWORD_RESET";

    @Schema(name = "ForgotPasswordRequest")
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
        // Luôn trả về success — không để lộ email nào đã đăng ký (User Enumeration Attack)
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            securityTokenPort.save(token, user.getId(), TOKEN_TYPE, TTL_15MIN_SECONDS);

            String resetLink = baseUrl + "/reset-password?token=" + token;
            emailPort.sendPasswordResetEmail(user.getEmail().value(), user.getDisplayName(), resetLink);
        });
    }
}
