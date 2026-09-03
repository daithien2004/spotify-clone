package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.FollowSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ViewFollowingUseCase {
    /** {@code userId} đang follow ai — mới nhất trước. */
    List<FollowSummaryResponse> execute(UUID userId);
}
