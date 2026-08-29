package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.repository.TrackRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTracksUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;

    @InjectMocks
    private ListTracksUseCaseImpl useCase;

    @Test
    void should_ReturnAllTracks_when_NoFilter() {
        Track a = Track.builder().id(UUID.randomUUID()).title("A").artist("X")
                .album("Ax").durationMs(1000L).artworkUrl("https://a.png").build();
        Track b = Track.builder().id(UUID.randomUUID()).title("B").artist("Y")
                .album("By").durationMs(2000L).artworkUrl("https://b.png").build();
        when(trackRepository.findAll()).thenReturn(List.of(a, b));

        List<TrackResponse> result = useCase.execute();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).title());
        assertEquals("B", result.get(1).title());
    }
}
