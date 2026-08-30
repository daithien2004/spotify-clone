package com.spotify.auth.application.usecase;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Cập nhật profile (displayName/avatarUrl) — cả 2 optional, ít nhất 1 non-null (PATCH phần). */
@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

  public record Request(
      @Size(max = 255) String displayName,
      @Size(max = 255) String avatarUrl) {}

  private final UserRepository userRepository;

  @Transactional
  public GetCurrentUserUseCase.UserResponse execute(UUID userId, Request request) {
    if (request.displayName() == null && request.avatarUrl() == null) {
      throw new DomainException("At least one field (displayName or avatarUrl) is required");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));

    String newName = request.displayName() != null ? request.displayName().trim() : user.getDisplayName();
    String newAvatar = request.avatarUrl() != null ? request.avatarUrl() : user.getAvatarUrl();
    user.updateProfile(newName, newAvatar);
    userRepository.save(user);

    return new GetCurrentUserUseCase.UserResponse(
        user.getId().toString(), user.getEmail().value(), user.getDisplayName(),
        user.getAvatarUrl(), user.isVerified(), user.isTwoFactorEnabled());
  }
}
