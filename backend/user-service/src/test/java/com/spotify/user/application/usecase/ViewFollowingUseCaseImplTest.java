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
class ViewFollowingUseCaseImplTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ViewFollowingUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID followeeId = UUID.randomUUID();

    @Test
    void should_ReturnFollowingSummaries_when_FollowingSomeone() {
        Follow follow = Follow.builder()
                .id(UUID.randomUUID())
                .followerId(userId)
                .followeeId(followeeId)
                .createdAt(OffsetDateTime.now())
                .build();
        User followee = User.builder()
                .id(followeeId)
                .email("t@example.com")
                .displayName("Taylor")
                .avatarUrl("/figma/taylor.png")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(followRepository.findAllByFollower(userId)).thenReturn(List.of(follow));
        when(userRepository.findById(followeeId)).thenReturn(java.util.Optional.of(followee));

        List<FollowSummaryResponse> result = useCase.execute(userId);

        assertEquals(1, result.size());
        FollowSummaryResponse summary = result.get(0);
        assertEquals(followeeId, summary.id());
        assertEquals("Taylor", summary.displayName());
        assertEquals("/figma/taylor.png", summary.avatarUrl());
        assertEquals(follow.getCreatedAt(), summary.followedAt());
    }

    @Test
    void should_ReturnEmpty_when_NotFollowingAnyone() {
        when(followRepository.findAllByFollower(userId)).thenReturn(List.of());

        List<FollowSummaryResponse> result = useCase.execute(userId);

        assertTrue(result.isEmpty());
    }
}
