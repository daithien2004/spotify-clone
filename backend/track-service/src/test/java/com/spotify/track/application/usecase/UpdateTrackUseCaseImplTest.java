package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTrackUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;

    @InjectMocks
    private UpdateTrackUseCaseImpl useCase;

    @Test
    void should_UpdateMetadata_when_TrackExists() {
        UUID id = UUID.randomUUID();
        Track existing = Track.builder()
                .id(id)
                .title("Old Title")
                .artist("Old Artist")
                .album("Old Album")
                .durationMs(1000L)
                .artworkUrl("https://old.png")
                .audioUrl("https://audio/old.mp3") // must survive the update
                .build();
        when(trackRepository.findById(id)).thenReturn(Optional.of(existing));
        when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TrackResponse result = useCase.execute(id,
                new CreateTrackRequest("New Title", "New Artist", "New Album", 2000L, "https://new.png"));

        assertEquals("New Title", result.title());
        assertEquals("New Artist", result.artist());
        assertEquals(2000L, result.durationMs());
        assertEquals("https://audio/old.mp3", result.audioUrl()); // audioUrl untouched
        ArgumentCaptor<Track> saved = ArgumentCaptor.forClass(Track.class);
        verify(trackRepository).save(saved.capture());
        assertEquals(id, saved.getValue().getId());
    }

    @Test
    void should_ThrowTrackNotFoundException_when_TrackMissing() {
        UUID id = UUID.randomUUID();
        when(trackRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TrackNotFoundException.class,
                () -> useCase.execute(id, new CreateTrackRequest("T", "A", null, 1000L, null)));
    }

    @Test
    void should_ThrowException_when_UpdatedTitleIsBlank() {
        UUID id = UUID.randomUUID();
        Track existing = Track.builder().id(id).title("Old").artist("A").durationMs(1000L).build();
        when(trackRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(id, new CreateTrackRequest("", "A", null, 1000L, null)));
    }
}