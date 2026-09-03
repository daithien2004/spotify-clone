package com.spotify.user.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Quan hệ follow: {@code followerId} theo dõi {@code followeeId}. */
@Getter
@Builder
public class Follow {
    private final UUID id;
    private final UUID followerId;
    private final UUID followeeId;
    private final OffsetDateTime createdAt;
}
