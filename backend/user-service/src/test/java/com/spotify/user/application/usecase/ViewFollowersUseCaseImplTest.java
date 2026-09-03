package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.FollowSummaryResponse;
import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewFollowersUseCaseImplTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ViewFollowersUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID followerId = UUID.randomUUID();

    @Test
    void should_ReturnFollowerSummaries_when_AuthorizedUserExists() {
        Follow follow = Follow.builder()
                .id(UUID.randomUUID())
                .followerId(followerId)
                .followeeId(userId)
                .createdAt(OffsetDateTime.now())
                .build();
        User follower = User.builder()
                .id(followerId)
                .email("f@example.com")
                .displayName("Bob")
                .avatarUrl("/figma/bob.png")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(followRepository.findAllByFollowee(userId)).thenReturn(List.of(follow));
        when(userRepository.findById(followerId)).thenReturn(java.util.Optional.of(follower));

        List<FollowSummaryResponse> result = useCase.execute(userId);

        assertEquals(1, result.size());
        FollowSummaryResponse summary = result.get(0);
        assertEquals(followerId, summary.id());
        assertEquals("Bob", summary.displayName());
        assertEquals("/figma/bob.png", summary.avatarUrl());
        assertEquals(follow.getCreatedAt(), summary.followedAt());
    }

    @Test
    void should_ReturnEmpty_when_NoFollowers() {
        when(followRepository.findAllByFollowee(userId)).thenReturn(List.of());

        List<FollowSummaryResponse> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }
}
