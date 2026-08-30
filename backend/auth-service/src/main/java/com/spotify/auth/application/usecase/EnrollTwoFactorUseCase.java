package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Bắt đầu đăng ký 2FA — sinh secret + lưu (CHƯA bật), trả QR cho FE. */
@Service
@RequiredArgsConstructor
public class EnrollTwoFactorUseCase {

  private static final String ISSUER = "Spotify Clone";

  public record EnrollResponse(String otpauthUrl, String qrDataUri) {}

  private final UserRepository userRepository;
  private final TotpPort totpPort;

  @Transactional
  public EnrollResponse execute(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.isTwoFactorEnabled()) {
      throw new DomainException("2FA is already enabled");
    }
    // ADR D2: lưu secret ngay để reload không mất QR, bật totpEnabled CHỈ khi verify code
    String secret = totpPort.generateSecret();
    String otpauthUri = totpPort.buildOtpAuthUri(user.getEmail().value(), ISSUER, secret);
    user.storePendingTotpSecret(secret);
    userRepository.save(user);
    return new EnrollResponse(otpauthUri, totpPort.generateQrDataUri(otpauthUri));
  }
}
