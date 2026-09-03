package com.spotify.user.infrastructure.event;

import com.spotify.user.domain.event.DomainEvent;
import com.spotify.user.domain.event.UserFollowed;
import com.spotify.user.domain.repository.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes domain events to Kafka. Follow events may feed recommendation/search.
 * Topic: {@code user.followed} (domain.md event map) — String key = followerId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserKafkaDomainEventPublisher implements DomainEventPublisher {

    static final String FOLLOW_TOPIC = "user.followed";

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

        if (event instanceof UserFollowed followed) {
            Map<String, Object> payload = Map.of(
                    "eventId", followed.getEventId(),
                    "occurredOn", followed.getOccurredOn(),
                    "followerId", followed.getFollowerId(),
                    "followeeId", followed.getFolloweeId());
            kafkaTemplate.send(FOLLOW_TOPIC, followed.getFollowerId().toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[USER_EVENT] Failed to publish follow event: {}", ex.getMessage());
                        }
                    });
        }
    }
}
