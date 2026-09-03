package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.event.UserFollowed;
import com.spotify.user.domain.exception.SelfFollowException;
import com.spotify.user.domain.exception.UserNotFoundException;
import com.spotify.user.domain.repository.DomainEventPublisher;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUserUseCaseImplTest {

    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private DomainEventPublisher domainEventPublisher;

    @InjectMocks private FollowUserUseCaseImpl useCase;

    private final UUID followerId = UUID.randomUUID();
    private final UUID followeeId = UUID.randomUUID();

    private User userOf(UUID id) {
        return User.builder()
                .id(id)
                .email("u@" + id + ".com")
                .displayName("User " + id)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void should_ThrowSelfFollow_when_FollowerEqualsFollowee() {
        assertThrows(SelfFollowException.class, () -> useCase.execute(followerId, followerId));
        verify(followRepository, never()).save(any());
    }

    @Test
    void should_ThrowUserNotFound_when_FolloweeMissing() {
        when(userRepository.findById(followeeId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(followerId, followeeId));
        verify(followRepository, never()).save(any());
    }

    @Test
    void should_SaveFollow_and_PublishEvent_when_Valid() {
        when(userRepository.findById(followeeId)).thenReturn(Optional.of(userOf(followeeId)));
        when(followRepository.findFollowing(followerId, followeeId)).thenReturn(Optional.empty());

        useCase.execute(followerId, followeeId);

        ArgumentCaptor<Follow> followCaptor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(followCaptor.capture());
        assertSame(followerId, followCaptor.getValue().getFollowerId());
        assertSame(followeeId, followCaptor.getValue().getFolloweeId());

        ArgumentCaptor<UserFollowed> eventCaptor = ArgumentCaptor.forClass(UserFollowed.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertSame(followerId, eventCaptor.getValue().getFollowerId());
        assertSame(followeeId, eventCaptor.getValue().getFolloweeId());
    }

    @Test
    void should_BeIdempotent_when_AlreadyFollowing() {
        Follow existing = Follow.builder()
                .id(UUID.randomUUID())
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(OffsetDateTime.now())
                .build();
        when(userRepository.findById(followeeId)).thenReturn(Optional.of(userOf(followeeId)));
        when(followRepository.findFollowing(followerId, followeeId)).thenReturn(Optional.of(existing));

        Follow result = useCase.execute(followerId, followeeId);

        assertSame(existing, result);
        verify(followRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }
}
