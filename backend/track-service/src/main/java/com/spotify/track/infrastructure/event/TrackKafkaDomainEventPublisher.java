package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.track.domain.event.DomainEvent;
import com.spotify.track.domain.repository.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Infrastructure adapter that publishes track domain events to Kafka. */
@Slf4j
@Component
public class TrackKafkaDomainEventPublisher implements DomainEventPublisher {

    public static final String TRACK_EVENTS_TOPIC = "spotify.track.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TrackEventEnvelopeMapper envelopeMapper;
    private final boolean kafkaEnabled;

    public TrackKafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                          TrackEventEnvelopeMapper envelopeMapper,
                                          @Value("${spring.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.envelopeMapper = envelopeMapper;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain Event Published: {} | ID: {} | At: {}",
                event.getClass().getSimpleName(), event.getEventId(), event.getOccurredOn());

        if (!kafkaEnabled) {
            return;
        }

        TrackEventEnvelope envelope = envelopeMapper.toEnvelope(event);
        kafkaTemplate.send(TRACK_EVENTS_TOPIC, envelope.eventId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        return;
                    }
                    log.error("[TRACK_EVENT] Failed to publish {} to Kafka: {}",
                            event.getClass().getSimpleName(), ex.getMessage());
                });
    }
}
