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

class DisableTwoFactorUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final DisableTwoFactorUseCase useCase =
      new DisableTwoFactorUseCase(userRepository, totpPort, auditPublisher);

  @Test
  void should_Disable_when_CodeMatches() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);

    useCase.execute(id, "123456");

    assertFalse(user.isTwoFactorEnabled());
    assertNull(user.getTotpSecret());
    verify(userRepository).save(user);
    verify(auditPublisher).publish(id.toString(), "user@example.com",
        SecurityAuditPublisher.EventType.TWO_FA_DISABLED, null, null, null);
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(id, "000000"));
    assertTrue(user.isTwoFactorEnabled());
    verify(userRepository, never()).save(any());
  }
}
