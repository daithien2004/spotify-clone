package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.EmailPort;
import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenPort tokenPort;

    @Mock
    private EmailPort emailPort;

    @Mock
    private SecurityAuditPublisher auditPublisher;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    void should_LoginSuccessfully_when_CredentialsAreValid() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("test@example.com", "Test1234", "127.0.0.1", "JUnit");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(new Email("test@example.com"))
                .password(new Password("Hashed1234"))
                .displayName("User Name")
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("Test1234", "Hashed1234")).thenReturn(true);
        when(tokenPort.generateToken(user)).thenReturn("fake-jwt-token");
        when(tokenPort.generateRefreshToken()).thenReturn("fake-refresh-token");
        when(tokenPort.getRefreshTokenExpirationMillis()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(auditPublisher).publish(any(), any(), any(), any(), any(), any());

        // When
        LoginUseCase.Response response = loginUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals("fake-jwt-token", response.accessToken());
        assertFalse(response.requires2fa());
    }

    @Test
    void should_ThrowException_when_PasswordIsInvalid() {
        // Test này expect throw exception — Đừng new Password() trực tiếp bên ngoài assertThrows
        assertThrows(DomainException.class, () -> {
            new Password("abc");  // chỉ chữ, không số → đúng là phải throw
        });
    }

    @Test
    void should_ThrowException_when_UserNotFound() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("nonexistent@example.com", "Test1234", null, null);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DomainException.class, () -> loginUseCase.execute(request));
    }
}
