package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.FollowSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ViewFollowersUseCase {
    /** Ai đang follow {@code userId} — mới nhất trước. */
    List<FollowSummaryResponse> execute(UUID userId);
}
