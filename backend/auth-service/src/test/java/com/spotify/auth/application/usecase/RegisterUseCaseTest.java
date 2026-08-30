package com.spotify.auth.application.usecase;

import java.util.UUID;

import com.spotify.auth.application.port.out.EmailPort;
import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.UserAlreadyExistsException;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenPort tokenPort;
    
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private SecurityTokenPort securityTokenPort;

    @Mock
    private EmailPort emailPort;

    @InjectMocks
    private RegisterUseCase registerUseCase;

    @Test
    void should_RegisterSuccessfully_when_UserDoesNotExist() {
        // Given // Thay bằng password có cả chữ lẫn số
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "Test1234", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoderPort.encode(anyString())).thenReturn("Hashed1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(UUID.randomUUID())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        });
        when(tokenPort.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // When
        RegisterUseCase.Response response = registerUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals("fake-jwt-token", response.accessToken());
        verify(userRepository).save(any(User.class));
        verify(domainEventPublisher).publish(any());
    }

    @Test
    void should_ThrowException_when_UserAlreadyExists() {
        // Given
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "Test1234", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> registerUseCase.execute(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_SendVerificationEmail_when_RegisterSucceeds() {
        // Given
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "Test1234", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoderPort.encode(anyString())).thenReturn("Hashed1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(UUID.randomUUID())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        });
        when(tokenPort.generateToken(any(User.class))).thenReturn("fake-jwt-token");
        doNothing().when(securityTokenPort).save(any(), any(), any(), anyLong());
        doNothing().when(emailPort).sendVerificationEmail(anyString(), anyString(), anyString());

        // When — auto-send sau khi lưu user + publish event (spec D6)
        registerUseCase.execute(request);

        // Then
        verify(securityTokenPort).save(any(), any(), eq("EMAIL_VERIFICATION"), eq(24L * 60 * 60));
        verify(emailPort).sendVerificationEmail(eq("test@example.com"), eq("User Name"),
                contains("/verify-email?token="));
    }
}
