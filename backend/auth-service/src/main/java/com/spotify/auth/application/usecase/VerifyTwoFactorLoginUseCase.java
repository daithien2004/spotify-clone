package com.spotify.auth.application.usecase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/** UseCase: Xác nhận mã TOTP khi login — mfaToken single-use (Redis 5 phút, ADR D1/D3). */
@Service
@RequiredArgsConstructor
public class VerifyTwoFactorLoginUseCase {

  public static final String TOKEN_TYPE = "MFA_CHALLENGE";
  public static final long TTL_SECONDS = 5 * 60L;

  public record Request(
      @NotBlank String mfaToken,
      @NotBlank @Size(min = 6, max = 6, message = "Code must be 6 digits") String code,
      @JsonIgnore String ipAddress,
      @JsonIgnore String userAgent) {}

  public record Response(
      @JsonIgnore String accessToken,
      @JsonIgnore String refreshToken,
      String userId,
      String email,
      String displayName,
      String avatarUrl,
      long expiresIn) {}

  private final UserRepository userRepository;
  private final SecurityTokenPort securityTokenPort;
  private final TotpPort totpPort;
  private final TokenPort tokenPort;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public Response execute(Request request) {
    UUID userId = securityTokenPort.findUserIdByToken(request.mfaToken(), TOKEN_TYPE)
        .orElseThrow(() -> new DomainException("2FA session expired. Please log in again."));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null
        || !user.isTwoFactorEnabled()
        || !totpPort.isValid(request.code(), user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    // Mã đúng → mfaToken single-use, xoá ngay. Mã sai → GIỮ token cho retry tới hết TTL.
    securityTokenPort.delete(request.mfaToken(), TOKEN_TYPE);

    String accessToken = tokenPort.generateToken(user);
    String refreshTokenStr = tokenPort.generateRefreshToken();
    RefreshToken rt = RefreshToken.builder()
        .token(refreshTokenStr)
        .userId(user.getId())
        .familyId(UUID.randomUUID())
        .ipAddress(request.ipAddress())
        .userAgent(request.userAgent())
        .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(tokenPort.getRefreshTokenExpirationMillis())))
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
    refreshTokenRepository.save(rt);

    // Đăng nhập 2FA thành công → LOGIN_SUCCESS audit (bước nhập mật khẩu KHÔNG audit — spec §7)
    auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.LOGIN_SUCCESS, request.ipAddress(), request.userAgent(), null);

    long expiresIn = tokenPort.getAccessTokenExpirationMillis() / 1000;
    return new Response(accessToken, refreshTokenStr, user.getId().toString(),
        user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl(), expiresIn);
  }
}
