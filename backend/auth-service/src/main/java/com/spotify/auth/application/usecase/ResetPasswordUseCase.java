package com.spotify.auth.application.usecase;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UseCase: Xác nhận reset mật khẩu bằng token.
 * Sau khi đổi màt khẩu: revoke toàn bộ refresh tokens (force logout tất cả thiết bị).
 */
@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private static final String TOKEN_TYPE = "PASSWORD_RESET";

    @Schema(name = "ResetPasswordRequest")
    public record Request(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {}

    private final UserRepository userRepository;
    private final SecurityTokenPort securityTokenPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditPublisher auditPublisher;

    @Transactional
    public void execute(Request request) {
        UUID userId = securityTokenPort.findUserIdByToken(request.token(), TOKEN_TYPE)
                .orElseThrow(() -> new DomainException("Invalid or expired reset token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        String encoded = passwordEncoderPort.encode(request.newPassword());
        user.updatePassword(new Password(encoded));
        userRepository.save(user);

        // Token đã dùng — xoá ngay lập tức
        securityTokenPort.delete(request.token(), TOKEN_TYPE);

        // Force logout tất cả thiết bị sau khi đổi mật khẩu (security best practice)
        refreshTokenRepository.revokeAllUserTokens(userId);

        auditPublisher.publish(userId.toString(), user.getEmail().value(),
                SecurityAuditPublisher.EventType.PASSWORD_RESET, null, null, null);
    }
}
