package com.spotify.playlist.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Application port to resolve a user's public display name from user-service
 * (cross-service HTTP). Kept out of domain to preserve Clean Architecture purity —
 * domain layer never depends on external services.
 */
public interface UserProfileClient {

    /**
     * Returns the display name of the user, or empty if the user is not known
     * (e.g. not yet synced into user-service via Kafka).
     */
    Optional<String> findDisplayName(UUID userId);
}
