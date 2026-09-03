package com.spotify.user.domain.event;

import java.util.UUID;

/** Emit khi một user follow một user khác (có thể cho recommendation/search). */
public class UserFollowed extends DomainEvent {
    private final UUID followerId;
    private final UUID followeeId;

    public UserFollowed(UUID followerId, UUID followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFolloweeId() {
        return followeeId;
    }
}
