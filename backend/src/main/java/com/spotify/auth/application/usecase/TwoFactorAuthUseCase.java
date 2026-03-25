package com.spotify.auth.application.usecase;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * UseCase: Thiết lập Two-Factor Authentication (TOTP).
 * Bước 1: setupTwoFa() — sinh secret, trả QR code URI cho user scan.
 * Bước 2: confirmTwoFa() — user nhập TOTP code xác nhận, mới bật 2FA.
 */
@Service
@RequiredArgsConstructor
public class TwoFactorAuthUseCase {

    public record SetupResponse(String secret, String qrCodeUri) {}

    private final UserRepository userRepository;
    private final QrGenerator qrGenerator;
    private final SecurityAuditPublisher auditPublisher;

    /**
     * Bước 1: Tạo TOTP secret mới cho user, trả về QR code URI.
     * 2FA chưa được bật — chỉ sau khi user xác nhận mã TOTP mới enable.
     */
    @Transactional
    public SetupResponse setupTwoFa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        // Lưu tạm secret NHƯNG chưa enable 2FA (phải confirm trước)
        // Ta dùng totpSecret store tạm, isTwoFaEnabled vẫn false
        user.enable2fa(secret);
        user.disable2fa(); // Xoá flag enable, chỉ giữ secret để verify
        // Compromise fix: store secret without enabling
        // We'll use a temp field approach — save secret, enable only on confirm
        // For simplicity we use a temp Redis key in more advanced implementation
        // Here we just store the secret in DB, enabled=false
        userRepository.save(user); // secret saved, twoFaEnabled=false

        QrData data = new QrData.Builder()
                .label(user.getEmail().value())
                .secret(secret)
                .issuer("Spotify Clone")
                .digits(6)
                .period(30)
                .build();

        try {
            String qrCodeUri = "data:image/png;base64," +
                    java.util.Base64.getEncoder().encodeToString(qrGenerator.generate(data));
            return new SetupResponse(secret, qrCodeUri);
        } catch (Exception e) {
            throw new DomainException("Failed to generate QR code");
        }
    }

    /**
     * Bước 2: User xác nhận TOTP code → bật 2FA chính thức.
     */
    @Transactional
    public void confirmTwoFa(UUID userId, @NotBlank String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new DomainException("2FA setup not initiated. Please call setup first.");
        }

        if (!verifyCode(user.getTotpSecret(), totpCode)) {
            throw new DomainException("Invalid TOTP code");
        }

        // TOTP code đúng → bật 2FA chính thức
        user.enable2fa(user.getTotpSecret());
        userRepository.save(user);

        auditPublisher.publish(userId.toString(), user.getEmail().value(),
                SecurityAuditPublisher.EventType.TWO_FA_ENABLED, null, null, null);
    }

    /**
     * Tắt 2FA. Yêu cầu nhập TOTP code xác nhận để tránh attacker vô hiệu hoá 2FA.
     */
    @Transactional
    public void disableTwoFa(UUID userId, @NotBlank String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        if (!user.isTwoFaEnabled()) {
            throw new DomainException("2FA is not enabled");
        }

        if (!verifyCode(user.getTotpSecret(), totpCode)) {
            throw new DomainException("Invalid TOTP code. Cannot disable 2FA.");
        }

        user.disable2fa();
        userRepository.save(user);

        auditPublisher.publish(userId.toString(), user.getEmail().value(),
                SecurityAuditPublisher.EventType.TWO_FA_DISABLED, null, null, null);
    }

    /**
     * Xác minh TOTP code khi login (dùng bởi LoginWith2FAUseCase).
     * Cho phép clock skew ±1 period (30s).
     */
    public boolean verifyCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }
}
