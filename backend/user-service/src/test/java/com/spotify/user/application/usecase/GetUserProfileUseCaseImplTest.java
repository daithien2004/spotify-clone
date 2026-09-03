package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.UserProfileResponse;
import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.exception.UserNotFoundException;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserProfileUseCaseImplTest {

    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @InjectMocks private GetUserProfileUseCaseImpl useCase;

    private final UUID id = UUID.randomUUID();

    private User userOf() {
        return User.builder()
                .id(id)
                .email("u@example.com")
                .displayName("Alice")
                .avatarUrl("/figma/avatar.png")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Follow followOf(UUID follower, UUID followee) {
        return Follow.builder()
                .id(UUID.randomUUID())
                .followerId(follower)
                .followeeId(followee)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void should_ReturnProfileWithCounts_when_UserExists() {
        when(userRepository.findById(id)).thenReturn(Optional.of(userOf()));
        when(followRepository.findAllByFollowee(id)).thenReturn(
                List.of(followOf(UUID.randomUUID(), id), followOf(UUID.randomUUID(), id)));
        when(followRepository.findAllByFollower(id)).thenReturn(
                List.of(followOf(id, UUID.randomUUID())));

        UserProfileResponse response = useCase.execute(id);

        assertEquals(id, response.id());
        assertEquals("Alice", response.displayName());
        assertEquals("/figma/avatar.png", response.avatarUrl());
        assertEquals(2, response.followersCount());
        assertEquals(1, response.followingCount());
    }

    @Test
    void should_ThrowUserNotFound_when_UserMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(id));
    }
}
