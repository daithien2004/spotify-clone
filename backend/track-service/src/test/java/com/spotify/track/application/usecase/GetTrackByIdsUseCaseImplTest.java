package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrackByIdsUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;

    @InjectMocks
    private GetTrackByIdsUseCaseImpl useCase;

    @Test
    void should_ReturnTracksInInputOrder_when_RepoReturnsShuffled() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        Track trackA = trackWith(idA, "A");
        Track trackB = trackWith(idB, "B");
        // Repo may return in any order — use case must rebuild to requested order [B, A]
        when(trackRepository.findAllByIds(List.of(idB, idA))).thenReturn(List.of(trackA, trackB));

        List<TrackResponse> result = useCase.execute(List.of(idB, idA));

        assertEquals(List.of(idB, idA), result.stream().map(TrackResponse::id).toList());
        assertEquals("B", result.get(0).title());
        assertEquals("A", result.get(1).title());
    }

    @Test
    void should_ThrowTrackNotFoundException_when_AnyIdIsMissing() {
        UUID present = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(trackRepository.findAllByIds(List.of(present, missing))).thenReturn(List.of(trackWith(present, "A")));

        assertThrows(TrackNotFoundException.class, () -> useCase.execute(List.of(present, missing)));
    }

    @Test
    void should_ReturnEmptyList_when_NoIdsRequested() {
        assertEquals(List.of(), useCase.execute(List.of()));
        verify(trackRepository, never()).findAllByIds(anyList());
    }

    private Track trackWith(UUID id, String title) {
        return Track.builder()
                .id(id)
                .title(title)
                .artist("Artist")
                .durationMs(1000L)
                .build();
    }
}