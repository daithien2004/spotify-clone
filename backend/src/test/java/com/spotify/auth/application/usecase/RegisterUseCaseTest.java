package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.PasswordEncoderPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.UserAlreadyExistsException;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenPort tokenPort;
    
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private RegisterUseCase registerUseCase;

    @Test
    void should_RegisterSuccessfully_when_UserDoesNotExist() {
        // Given
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "password123", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoderPort.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "password123", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> registerUseCase.execute(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
