package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Tắt 2FA — verify code hiện tại trước khi xoá secret. */
@Service
@RequiredArgsConstructor
public class DisableTwoFactorUseCase {

  private final UserRepository userRepository;
  private final TotpPort totpPort;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public void execute(UUID userId, String code) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null) {
      throw new DomainException("2FA is not enabled");
    }
    if (!totpPort.isValid(code, user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    user.disable2fa(); // xoá secret + off
    userRepository.save(user);
    // MFA_CHALLENGE cũ còn hiệu lực (≤5 phút) không phải lo — verify-login guard tự chặn
    // (user.getTotpSecret()==null → "Invalid 2FA code"). SecurityTokenPort không có delete-by-user,
    // TTL 5 phút tự dọn — ghi chú này thay cho việc cố xoá theo user (spec §7).
    auditPublisher.publish(userId.toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.TWO_FA_DISABLED, null, null, null);
  }
}
