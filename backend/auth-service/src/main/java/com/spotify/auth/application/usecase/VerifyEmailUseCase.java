package com.spotify.auth.application.usecase;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UseCase: Xác thực email bằng token được gửi qua email.
 * Token chỉ dùng được 1 lần (xoá ngay sau khi dùng).
 */
@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private static final String TOKEN_TYPE = "EMAIL_VERIFICATION";

    @Schema(name = "VerifyEmailRequest")
    public record Request(@NotBlank String token) {}

    private final UserRepository userRepository;
    private final SecurityTokenPort securityTokenPort;
    private final SecurityAuditPublisher auditPublisher;

    @Transactional
    public void execute(Request request) {
        UUID userId = securityTokenPort.findUserIdByToken(request.token(), TOKEN_TYPE)
                .orElseThrow(() -> new DomainException("Invalid or expired verification token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        user.verifyEmail();
        userRepository.save(user);

        // Token đã dùng xong — xoá để ngăn dùng lại
        securityTokenPort.delete(request.token(), TOKEN_TYPE);

        auditPublisher.publish(userId.toString(), user.getEmail().value(),
                SecurityAuditPublisher.EventType.EMAIL_VERIFIED, null, null,
                "Email successfully verified");
    }
}
