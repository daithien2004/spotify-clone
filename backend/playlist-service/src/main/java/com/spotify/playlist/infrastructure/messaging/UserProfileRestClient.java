package com.spotify.playlist.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.spotify.playlist.application.port.UserProfileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves display names from user-service over HTTP (cross-service call).
 * user-service wraps success in {success, data:{displayName, ...}}; a missing
 * user (not yet synced via Kafka) returns 404, which we surface as empty so the
 * use case can fall back to a stable label instead of failing playlist creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileRestClient implements UserProfileClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.user-service.base-url}")
    private String userServiceBaseUrl;

    @Override
    public Optional<String> findDisplayName(UUID userId) {
        try {
            JsonNode body = restClientBuilder.build()
                    .get()
                    .uri(userServiceBaseUrl + "/api/v1/users/{id}", userId)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = body == null ? null : body.get("data");
            if (data == null || data.get("displayName") == null || data.get("displayName").isNull()) {
                return Optional.empty();
            }
            return Optional.of(data.get("displayName").asText());
        } catch (Exception ex) {
            // user-service may be down or the user not yet projected — never block creation
            log.warn("Could not resolve display name for user {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }
}
