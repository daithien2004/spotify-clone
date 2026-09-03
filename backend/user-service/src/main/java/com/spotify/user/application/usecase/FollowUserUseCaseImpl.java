package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.event.UserFollowed;
import com.spotify.user.domain.exception.SelfFollowException;
import com.spotify.user.domain.exception.UserNotFoundException;
import com.spotify.user.domain.repository.DomainEventPublisher;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowUserUseCaseImpl implements FollowUserUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public Follow execute(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException();
        }

        // Đối tượng follow phải tồn tại trong user_db (projection từ Kafka).
        User target = userRepository.findById(followeeId)
                .orElseThrow(() -> new UserNotFoundException(followeeId));

        // Idempotent: nếu đã follow thì trả relation hiện có, không tạo trùng.
        return followRepository.findFollowing(followerId, followeeId)
                .orElseGet(() -> {
                    Follow follow = Follow.builder()
                            .id(UUID.randomUUID())
                            .followerId(followerId)
                            .followeeId(target.getId())
                            .createdAt(OffsetDateTime.now())
                            .build();
                    followRepository.save(follow);
                    domainEventPublisher.publish(new UserFollowed(followerId, followeeId));
                    return follow;
                });
    }
}
