package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnrollTwoFactorUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final EnrollTwoFactorUseCase useCase = new EnrollTwoFactorUseCase(userRepository, totpPort);

  @Test
  void should_ReturnOtpAuthAndQr_when_UserExists() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.generateSecret()).thenReturn("JBSWY3DPEHPK3PXP");
    when(totpPort.buildOtpAuthUri("user@example.com", "Spotify Clone", "JBSWY3DPEHPK3PXP"))
        .thenReturn("otpauth://totp/test");
    when(totpPort.generateQrDataUri("otpauth://totp/test")).thenReturn("data:image/png;base64,x");

    var response = useCase.execute(id);

    assertEquals("otpauth://totp/test", response.otpauthUrl());
    assertEquals("data:image/png;base64,x", response.qrDataUri());
    // ADR D2: secret lưu ngay, CHƯA bật — bật khi verify code
    assertFalse(user.isTwoFactorEnabled());
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
    verify(userRepository).save(user);
  }

  @Test
  void should_ThrowException_when_UserNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(DomainException.class, () -> useCase.execute(id));
  }

  @Test
  void should_ThrowException_when_AlreadyEnabled() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    assertThrows(DomainException.class, () -> useCase.execute(id));
  }
}
