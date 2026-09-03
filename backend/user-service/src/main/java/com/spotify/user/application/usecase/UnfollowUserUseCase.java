package com.spotify.user.application.usecase;

import java.util.UUID;

public interface UnfollowUserUseCase {
    /** follower bỏ follow followee; no-op nếu chưa follow (idempotent). */
    void execute(UUID followerId, UUID followeeId);
}
