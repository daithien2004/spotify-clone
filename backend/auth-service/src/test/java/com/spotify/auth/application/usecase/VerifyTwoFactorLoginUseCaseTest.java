package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifyTwoFactorLoginUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final SecurityTokenPort securityTokenPort = mock(SecurityTokenPort.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final TokenPort tokenPort = mock(TokenPort.class);
  private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final VerifyTwoFactorLoginUseCase useCase =
      new VerifyTwoFactorLoginUseCase(userRepository, securityTokenPort, totpPort,
          tokenPort, refreshTokenRepository, auditPublisher);

  private User twoFaUser() {
    User user = User.builder()
        .id(UUID.randomUUID())
        .email(new Email("user@example.com"))
        .displayName("User")
        .totpSecret("JBSWY3DPEHPK3PXP")
        .build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    return user;
  }

  @Test
  void should_IssueTokens_when_MfaTokenAndCodeValid() {
    User user = twoFaUser();
    when(securityTokenPort.findUserIdByToken("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);
    when(tokenPort.generateToken(user)).thenReturn("access-token");
    when(tokenPort.generateRefreshToken()).thenReturn("refresh-token");
    when(tokenPort.getAccessTokenExpirationMillis()).thenReturn(900000L);
    when(tokenPort.getRefreshTokenExpirationMillis()).thenReturn(604800000L);
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(auditPublisher).publish(any(), any(), any(), any(), any(), any());

    var response = useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("mfa-1", "123456", "127.0.0.1", "UA"));

    assertEquals(user.getId().toString(), response.userId());
    assertEquals("access-token", response.accessToken());
    verify(securityTokenPort).delete("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE);
    verify(refreshTokenRepository).save(any());
    verify(auditPublisher).publish(user.getId().toString(), "user@example.com",
        SecurityAuditPublisher.EventType.LOGIN_SUCCESS, "127.0.0.1", "UA", null);
  }

  @Test
  void should_Throw_when_MfaTokenInvalid() {
    when(securityTokenPort.findUserIdByToken("bad", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.empty());
    assertThrows(DomainException.class, () -> useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("bad", "123456", "127.0.0.1", "UA")));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    User user = twoFaUser();
    when(securityTokenPort.findUserIdByToken("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("mfa-1", "000000", "127.0.0.1", "UA")));
    // Token KHÔNG bị xoá khi code sai — cho retry tới hết TTL 5 phút (ADR D3)
    verify(securityTokenPort, never()).delete(any(), any());
    verify(refreshTokenRepository, never()).save(any());
  }
}
