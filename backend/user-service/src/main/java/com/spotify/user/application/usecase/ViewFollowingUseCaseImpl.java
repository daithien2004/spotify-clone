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
public class ViewFollowingUseCaseImpl implements ViewFollowingUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FollowSummaryResponse> execute(UUID userId) {
        return followRepository.findAllByFollower(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private FollowSummaryResponse toSummary(Follow follow) {
        User followee = userRepository.findById(follow.getFolloweeId()).orElseThrow();
        return FollowSummaryResponse.of(followee, follow);
    }
}
