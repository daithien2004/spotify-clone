package com.spotify.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The Kafka envelope must survive a Jackson round-trip (producer JsonSerializer ↔ consumer JsonDeserializer). */
class TrackEventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_SerializeAndDeserialize_when_EnvelopeHasFullPayload() throws Exception {
        TrackEventEnvelope envelope = new TrackEventEnvelope(
                TrackEventType.TRACK_UPLOADED,
                "12e0c789-0b18-46cc-9a4a-b106e5bf1f1e",
                "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload(
                        "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b",
                        "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                        182_000L, "https://artwork/free-spirit.png", "https://audio/free-spirit.mp3"));

        String json = objectMapper.writeValueAsString(envelope);
        TrackEventEnvelope back = objectMapper.readValue(json, TrackEventEnvelope.class);

        assertEquals(TrackEventType.TRACK_UPLOADED, back.eventType());
        assertNotNull(back.track());
        assertEquals("Free Spirit", back.track().title());
        assertEquals(182_000L, back.track().durationMs());
    }

    @Test
    void should_DeserializeAudioUploadEnvelope_when_OnlyIdPresent() throws Exception {
        String json = """
                {"eventType":"TRACK_AUDIO_UPLOADED","eventId":"uuid-1","occurredOn":"2026-08-29T10:15:30",
                 "track":{"id":"9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"}}

                """;

        TrackEventEnvelope envelope = new ObjectMapper().readValue(json, TrackEventEnvelope.class);

        assertEquals(TrackEventType.TRACK_AUDIO_UPLOADED, envelope.eventType());
        assertEquals("9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b", envelope.track().id());
    }
}