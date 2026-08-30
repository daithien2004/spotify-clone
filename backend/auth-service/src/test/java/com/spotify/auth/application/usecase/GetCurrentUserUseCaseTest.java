package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCurrentUserUseCaseTest {

  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final GetCurrentUserUseCase useCase = new GetCurrentUserUseCase(userRepository);

  @Test
  void should_ReturnProfileWithStateFlags_when_UserExists() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id)
        .email(new Email("u@e.com"))
        .displayName("U")
        .totpSecret("SEC")
        .build();
    user.verifyEmail();
    user.enable2fa("SEC");
    Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id);

    assertTrue(response.success());
    assertEquals(true, response.data().emailVerified());
    assertEquals(true, response.data().twoFactorEnabled());
  }
}
