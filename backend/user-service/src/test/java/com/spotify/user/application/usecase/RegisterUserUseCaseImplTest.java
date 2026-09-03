package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.event.UserRegisteredEvent;
import com.spotify.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private RegisterUserUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();

    private UserRegisteredEvent eventOf() {
        return new UserRegisteredEvent(
                UUID.randomUUID(), LocalDateTime.now(), userId, "new@example.com", "New User");
    }

    @Test
    void should_SaveNewUser_when_UserIdUnknown() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        useCase.execute(eventOf());

        verify(userRepository).save(argThat(u ->
                u.getId().equals(userId)
                        && u.getEmail().equals("new@example.com")
                        && u.getDisplayName().equals("New User")));
    }

    @Test
    void should_NotSave_when_UserAlreadyExists() {
        User existing = User.builder()
                .id(userId)
                .email("new@example.com")
                .displayName("Existing")
                .createdAt(java.time.OffsetDateTime.now())
                .updatedAt(java.time.OffsetDateTime.now())
                .build();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(existing));

        useCase.execute(eventOf());

        verify(userRepository, never()).save(any(User.class));
    }
}
