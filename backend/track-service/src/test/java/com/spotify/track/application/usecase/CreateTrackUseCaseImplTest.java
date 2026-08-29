package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.event.TrackUploaded;
import com.spotify.track.domain.repository.DomainEventPublisher;
import com.spotify.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTrackUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private CreateTrackUseCaseImpl useCase;

    @Test
    void should_CreateTrack_and_PublishEvent_when_RequestIsValid() {
        when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Track result = useCase.execute(new CreateTrackRequest("Blinding Lights", "The Weeknd",
                "After Hours", 200_000L, "https://artwork/blinding-lights.png"));

        assertNotNull(result.getId());
        assertEquals("Blinding Lights", result.getTitle());
        assertEquals("The Weeknd", result.getArtist());
        assertEquals(200_000L, result.getDurationMs());
        verify(trackRepository).save(any());

        ArgumentCaptor<TrackUploaded> eventCaptor = ArgumentCaptor.forClass(TrackUploaded.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals(result.getId(), eventCaptor.getValue().getTrackId());
        assertEquals("The Weeknd", eventCaptor.getValue().getArtist());
    }

    @Test
    void should_ThrowException_when_TitleIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new CreateTrackRequest(" ", "artist", null, 1000L, null)));
    }

    @Test
    void should_ThrowException_when_ArtistIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new CreateTrackRequest("title", "", null, 1000L, null)));
    }

    @Test
    void should_ThrowException_when_DurationMsIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new CreateTrackRequest("title", "artist", null, -1L, null)));
    }

    @Test
    void should_ThrowException_when_DurationMsIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new CreateTrackRequest("title", "artist", null, null, null)));
    }
}