package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateProfileUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final UpdateProfileUseCase useCase = new UpdateProfileUseCase(userRepository);

  @Test
  void should_UpdateDisplayName_when_Provided() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .displayName("Old").avatarUrl(null).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id, new UpdateProfileUseCase.Request("New Name", null));

    assertEquals("New Name", response.displayName());
    assertNull(response.avatarUrl());
    assertFalse(response.twoFactorEnabled());
    verify(userRepository).save(user);
  }

  @Test
  void should_KeepDisplayName_when_OnlyAvatarProvided() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("u@e.com"))
        .displayName("Keep").avatarUrl(null).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id, new UpdateProfileUseCase.Request(null, "https://i.img/a.png"));

    assertEquals("Keep", response.displayName());
    assertEquals("https://i.img/a.png", response.avatarUrl());
  }

  @Test
  void should_Throw_when_UserNotFound() {
    when(userRepository.findById(any())).thenReturn(Optional.empty());
    assertThrows(DomainException.class,
        () -> useCase.execute(UUID.randomUUID(), new UpdateProfileUseCase.Request("X", "url")));
  }

  @Test
  void should_Throw_when_BothFieldsNull() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.of(User.builder().id(id).build()));
    assertThrows(DomainException.class,
        () -> useCase.execute(id, new UpdateProfileUseCase.Request(null, null)));
  }
}
