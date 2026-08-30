package com.spotify.search.infrastructure.kafka;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.application.usecase.IndexTrackUseCase;
import com.spotify.search.application.usecase.RemoveTrackUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackEventConsumerTest {

    @Mock
    private IndexTrackUseCase indexTrackUseCase;
    @Mock
    private RemoveTrackUseCase removeTrackUseCase;

    @InjectMocks
    private TrackEventConsumer consumer;

    private TrackEventEnvelope envelope(TrackEventType type, String id) {
        return new TrackEventEnvelope(type, "evt-1", "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload(id, "Free Spirit", "Khalid",
                        "Free Spirit (Explicit)", 182_000L, "https://artwork.png", null));
    }

    @Test
    void should_Index_when_Uploaded() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_UPLOADED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase).execute(any(IndexTrackCommand.class));
        verify(removeTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Index_when_Updated() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_UPDATED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase).execute(any(IndexTrackCommand.class));
        verify(removeTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Remove_when_Removed() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_REMOVED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(removeTrackUseCase).execute(any(RemoveTrackCommand.class));
        verify(indexTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Ignore_when_AudioUploaded() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_AUDIO_UPLOADED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase, never()).execute(any());
        verify(removeTrackUseCase, never()).execute(any());
    }
}