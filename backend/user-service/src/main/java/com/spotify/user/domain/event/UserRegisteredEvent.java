package com.spotify.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload Kafka topic {@code user.registered} — do auth-service publish
 * (serialize từ com.spotify.auth.domain.event.UserRegistered qua JsonSerializer).
 * Field name khớp auth event để ObjectMapper deserialize trực tiếp.
 */
public record UserRegisteredEvent(
        UUID eventId,
        LocalDateTime occurredOn,
        UUID userId,
        String email,
        String displayName
) {
}
