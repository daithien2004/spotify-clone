package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.track.domain.event.DomainEvent;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.event.TrackUpdated;
import com.spotify.track.domain.event.TrackUploaded;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps track domain events onto the shared Kafka envelope. */
@Component
public class TrackEventEnvelopeMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TrackEventEnvelope toEnvelope(DomainEvent event) {
        if (event instanceof TrackUploaded uploaded) {
            return fullPayloadEnvelope(TrackEventType.TRACK_UPLOADED, event,
                    uploaded.getTrackId(), uploaded.getTitle(), uploaded.getArtist(), uploaded.getAlbum(),
                    uploaded.getDurationMs(), uploaded.getArtworkUrl(), uploaded.getAudioUrl());
        }
        if (event instanceof TrackUpdated updated) {
            return fullPayloadEnvelope(TrackEventType.TRACK_UPDATED, event,
                    updated.getTrackId(), updated.getTitle(), updated.getArtist(), updated.getAlbum(),
                    updated.getDurationMs(), updated.getArtworkUrl(), updated.getAudioUrl());
        }
        if (event instanceof TrackAudioUploaded audioUploaded) {
            return idOnlyEnvelope(event, audioUploaded.getTrackId());
        }
        throw new IllegalArgumentException(
                "Unsupported domain event: " + event.getClass().getSimpleName());
    }

    private TrackEventEnvelope fullPayloadEnvelope(TrackEventType type, DomainEvent event,
                                                   UUID trackId, String title, String artist,
                                                   String album, Long durationMs, String artworkUrl,
                                                   String audioUrl) {
        return new TrackEventEnvelope(type, event.getEventId().toString(),
                event.getOccurredOn().format(ISO),
                new TrackEventEnvelope.TrackPayload(trackId.toString(), title, artist, album,
                        durationMs, artworkUrl, audioUrl));
    }

    private TrackEventEnvelope idOnlyEnvelope(DomainEvent event, UUID trackId) {
        return new TrackEventEnvelope(TrackEventType.TRACK_AUDIO_UPLOADED,
                event.getEventId().toString(), event.getOccurredOn().format(ISO),
                new TrackEventEnvelope.TrackPayload(trackId.toString(), null, null, null,
                        null, null, null));
    }
}
