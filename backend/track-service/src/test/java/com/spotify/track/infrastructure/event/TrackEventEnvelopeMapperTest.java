package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.event.TrackUpdated;
import com.spotify.track.domain.event.TrackUploaded;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackEventEnvelopeMapperTest {

    private final TrackEventEnvelopeMapper mapper = new TrackEventEnvelopeMapper();

    @Test
    void should_MapUploaded_toEnvelope() {
        UUID id = UUID.randomUUID();
        TrackUploaded event = new TrackUploaded(id, "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                182_000L, "https://artwork.png", null);

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_UPLOADED, e.eventType());
        assertEquals(id.toString(), e.track().id());
        assertEquals("Khalid", e.track().artist());
        assertEquals(182_000L, e.track().durationMs());
    }

    @Test
    void should_MapUpdated_toEnvelope() {
        TrackUpdated event = new TrackUpdated(UUID.randomUUID(), "New Title", "Artist", "Album",
                100L, "", "");

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_UPDATED, e.eventType());
        assertEquals("New Title", e.track().title());
    }

    @Test
    void should_MapAudioUploaded_toEnvelopeWithOnlyId() {
        UUID id = UUID.randomUUID();
        TrackAudioUploaded event = new TrackAudioUploaded(id);

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_AUDIO_UPLOADED, e.eventType());
        assertEquals(id.toString(), e.track().id());
        assertEquals(null, e.track().title());
    }

    @Test
    void should_Throw_when_EventTypeIsUnsupported() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEnvelope(new com.spotify.track.domain.event.DomainEvent() {}));
    }
}