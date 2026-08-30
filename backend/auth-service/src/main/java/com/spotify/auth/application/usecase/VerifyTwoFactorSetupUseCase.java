package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Xác nhận mã TOTP khi setup → kích hoạt 2FA + audit. */
@Service
@RequiredArgsConstructor
public class VerifyTwoFactorSetupUseCase {

  public record Request(
      @NotBlank @Size(min = 6, max = 6, message = "Code must be 6 digits") String code) {}

  private final UserRepository userRepository;
  private final TotpPort totpPort;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public void execute(UUID userId, String code) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null) {
      throw new DomainException("No pending 2FA enrollment. Start enrollment first.");
    }
    if (!totpPort.isValid(code, user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    user.enable2fa(user.getTotpSecret()); // giữ secret + bật totpEnabled
    userRepository.save(user);
    auditPublisher.publish(userId.toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.TWO_FA_ENABLED, null, null, null);
  }
}
