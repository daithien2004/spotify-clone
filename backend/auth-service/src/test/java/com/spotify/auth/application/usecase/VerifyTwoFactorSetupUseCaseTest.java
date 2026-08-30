package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifyTwoFactorSetupUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final VerifyTwoFactorSetupUseCase useCase =
      new VerifyTwoFactorSetupUseCase(userRepository, totpPort, auditPublisher);

  @Test
  void should_Enable2fa_when_CodeMatches() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);

    useCase.execute(id, "123456");

    assertTrue(user.isTwoFactorEnabled());
    verify(userRepository).save(user);
    verify(auditPublisher).publish(id.toString(), "user@example.com",
        SecurityAuditPublisher.EventType.TWO_FA_ENABLED, null, null, null);
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(id, "000000"));
    assertFalse(user.isTwoFactorEnabled());
    verify(userRepository, never()).save(any());
  }

  @Test
  void should_Throw_when_NoSecretYet() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build(); // totpSecret null
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    assertThrows(DomainException.class, () -> useCase.execute(id, "123456"));
    verify(userRepository, never()).save(any());
  }
}
