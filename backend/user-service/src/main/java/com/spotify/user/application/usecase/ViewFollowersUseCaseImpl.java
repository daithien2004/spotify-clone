package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.FollowSummaryResponse;
import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViewFollowersUseCaseImpl implements ViewFollowersUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FollowSummaryResponse> execute(UUID userId) {
        return followRepository.findAllByFollowee(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private FollowSummaryResponse toSummary(Follow follow) {
        User follower = userRepository.findById(follow.getFollowerId()).orElseThrow();
        return FollowSummaryResponse.of(follower, follow);
    }
}
