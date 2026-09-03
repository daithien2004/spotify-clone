package com.spotify.auth.infrastructure.event;

import com.spotify.auth.domain.event.DomainEvent;
import com.spotify.auth.domain.event.UserRegistered;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes auth domain events. {@code User.Registered} goes to Kafka topic
 * {@code user.registered} for user-service/email-service (domain.md event map),
 * keeping the local log for observability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLogDomainEventPublisher implements DomainEventPublisher {

    static final String USER_REGISTERED_TOPIC = "user.registered";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain Event Published: {} | ID: {} | At: {}",
                event.getClass().getSimpleName(), event.getEventId(), event.getOccurredOn());

        if (!kafkaEnabled) {
            return;
        }

        // JsonSerializer dùng getters của UserRegistered (eventId, occurredOn, userId, email)
        // → JSON khớp record UserRegisteredEvent trong user-service.
        if (event instanceof UserRegistered registered) {
            kafkaTemplate.send(USER_REGISTERED_TOPIC, registered.getUserId().toString(), registered)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[USER_EVENT] Failed to publish user.registered: {}", ex.getMessage());
                        }
                    });
        }
    }
}
