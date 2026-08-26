package com.spotify.auth.application.usecase;

import com.spotify.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public Response execute(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new Response(
                        true,
                        new UserResponse(
                                user.getId().toString(),
                                user.getEmail().value(),
                                user.getDisplayName(),
                                user.getAvatarUrl()
                        )
                ))
                .orElse(new Response(false, null));
    }

    public record Response(boolean success, UserResponse data) {}
    public record UserResponse(String id, String email, String displayName, String avatarUrl) {}
}
