package com.spotify.user.application.usecase;

import com.spotify.user.domain.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnfollowUserUseCaseImpl implements UnfollowUserUseCase {

    private final FollowRepository followRepository;

    @Override
    @Transactional
    public void execute(UUID followerId, UUID followeeId) {
        followRepository.findFollowing(followerId, followeeId)
                .ifPresent(followRepository::delete);
    }
}
