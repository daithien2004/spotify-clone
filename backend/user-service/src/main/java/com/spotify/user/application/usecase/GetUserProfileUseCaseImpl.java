package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.UserProfileResponse;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.exception.UserNotFoundException;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserProfileUseCaseImpl implements GetUserProfileUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        int followersCount = followRepository.findAllByFollowee(userId).size();
        int followingCount = followRepository.findAllByFollower(userId).size();
        return UserProfileResponse.from(user, followersCount, followingCount);
    }
}
