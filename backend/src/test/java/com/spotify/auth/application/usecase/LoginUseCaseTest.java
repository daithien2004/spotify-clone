package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenPort tokenPort;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    void should_LoginSuccessfully_when_CredentialsAreValid() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("test@example.com", "password123");
        User user = User.builder()
                .email(new Email("test@example.com"))
                .password(new Password("hashed-password"))
                .displayName("User Name")
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("password123", "hashed-password")).thenReturn(true);
        when(tokenPort.generateToken(user)).thenReturn("fake-jwt-token");

        // When
        LoginUseCase.Response response = loginUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals("fake-jwt-token", response.accessToken());
    }

    @Test
    void should_ThrowException_when_PasswordIsInvalid() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("test@example.com", "wrong-password");
        User user = User.builder()
                .email(new Email("test@example.com"))
                .password(new Password("hashed-password"))
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("wrong-password", "hashed-password")).thenReturn(false);

        // When & Then
        assertThrows(DomainException.class, () -> loginUseCase.execute(request));
    }

    @Test
    void should_ThrowException_when_UserNotFound() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("nonexistent@example.com", "password123");
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DomainException.class, () -> loginUseCase.execute(request));
    }
}
