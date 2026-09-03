package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.repository.FollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnfollowUserUseCaseImplTest {

    @Mock private FollowRepository followRepository;
    @InjectMocks private UnfollowUserUseCaseImpl useCase;

    private final UUID followerId = UUID.randomUUID();
    private final UUID followeeId = UUID.randomUUID();

    @Test
    void should_DeleteFollow_when_FollowExists() {
        Follow follow = Follow.builder()
                .id(UUID.randomUUID())
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(OffsetDateTime.now())
                .build();
        when(followRepository.findFollowing(followerId, followeeId)).thenReturn(Optional.of(follow));

        useCase.execute(followerId, followeeId);

        verify(followRepository).delete(follow);
    }

    @Test
    void should_BeNoOp_when_NotFollowing() {
        when(followRepository.findFollowing(followerId, followeeId)).thenReturn(Optional.empty());

        useCase.execute(followerId, followeeId);

        verify(followRepository, never()).delete(any());
    }
}
