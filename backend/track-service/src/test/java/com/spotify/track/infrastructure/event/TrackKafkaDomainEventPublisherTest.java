package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.track.domain.event.TrackUploaded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackKafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TrackEventEnvelopeMapper envelopeMapper;

    private TrackKafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // Explicit ctor pattern (portable, no reflection): the @Value flag is passed in.
    }

    private TrackUploaded anyUploaded() {
        return new TrackUploaded(UUID.randomUUID(), "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                182_000L, "https://artwork.png", null);
    }

    @Test
    void should_SendEnvelope_when_KafkaEnabled() {
        TrackEventEnvelope envelope = new TrackEventEnvelope(
                TrackEventType.TRACK_UPLOADED, "evt-1", "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload("id-1", "Free Spirit", "Khalid",
                        "Free Spirit (Explicit)", 182_000L, "https://artwork.png", null));
        when(envelopeMapper.toEnvelope(any())).thenReturn(envelope);
        when(kafkaTemplate.send(anyString(), anyString(), any(TrackEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher = new TrackKafkaDomainEventPublisher(kafkaTemplate, envelopeMapper, true);

        publisher.publish(anyUploaded());

        verify(kafkaTemplate).send(eq(TrackKafkaDomainEventPublisher.TRACK_EVENTS_TOPIC),
                eq("evt-1"), eq(envelope));
    }

    @Test
    void should_NotSend_when_KafkaDisabled() {
        publisher = new TrackKafkaDomainEventPublisher(kafkaTemplate, envelopeMapper, false);

        publisher.publish(anyUploaded());

        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }
}