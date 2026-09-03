package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.event.UserRegisteredEvent;
import com.spotify.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void execute(UserRegisteredEvent event) {
        // Idempotent theo email — user-service chỉ project profile, không phải identity source.
        if (userRepository.findByEmail(event.email()).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                        .id(event.userId())
                        .email(event.email())
                        .displayName(event.displayName())
                        .avatarUrl(null)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build());
    }
}
