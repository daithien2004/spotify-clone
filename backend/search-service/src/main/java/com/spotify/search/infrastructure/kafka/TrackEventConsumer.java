package com.spotify.search.infrastructure.kafka;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.application.usecase.IndexTrackUseCase;
import com.spotify.search.application.usecase.RemoveTrackUseCase;
import com.spotify.search.domain.entity.TrackSearchDocument;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes track events from the shared topic and keeps the ES index in sync.
 * TRACK_AUDIO_UPLOADED does not affect the index (spec §3) — logged only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrackEventConsumer {

    private static final String TRACK_EVENTS_TOPIC = "spotify.track.events";

    private final IndexTrackUseCase indexTrackUseCase;
    private final RemoveTrackUseCase removeTrackUseCase;

    @KafkaListener(topics = TRACK_EVENTS_TOPIC, groupId = "search-service-group")
    public void onTrackEvent(TrackEventEnvelope envelope) {
        log.debug("Received track event: {} | eventId={}", envelope.eventType(), envelope.eventId());
        switch (envelope.eventType()) {
            case TRACK_UPLOADED, TRACK_UPDATED -> indexTrackUseCase.execute(
                    new IndexTrackCommand(toDocument(envelope.track())));
            case TRACK_REMOVED -> removeTrackUseCase.execute(
                    new RemoveTrackCommand(UUID.fromString(envelope.track().id())));
            case TRACK_AUDIO_UPLOADED -> log.debug("Audio upload event — index unaffected");
        }
    }

    private TrackSearchDocument toDocument(TrackEventEnvelope.TrackPayload track) {
        return new TrackSearchDocument(
                UUID.fromString(track.id()),
                track.title(),
                track.artist(),
                track.album(),
                track.durationMs(),
                track.artworkUrl(),
                track.audioUrl());
    }
}