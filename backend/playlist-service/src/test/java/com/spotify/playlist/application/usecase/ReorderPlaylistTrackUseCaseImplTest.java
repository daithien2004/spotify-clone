package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.ReorderPlaylistTrackRequest;
import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.event.TrackMovedInPlaylist;
import com.spotify.playlist.domain.repository.DomainEventPublisher;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.service.LexoRankService;
import com.spotify.playlist.domain.valueobject.LexoRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderPlaylistTrackUseCaseImplTest {

    @Spy
    private final LexoRankService lexoRankService = new LexoRankService();

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;
    @Mock
    private RebalancePlaylistUseCase rebalancePlaylistUseCase;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ReorderPlaylistTrackUseCaseImpl useCase;

    private PlaylistTrack trackIn(UUID playlistId) {
        return PlaylistTrack.builder()
                .id(UUID.randomUUID())
                .playlistId(playlistId)
                .trackId(UUID.randomUUID())
                .lexoRank(new LexoRank("m"))
                .addedAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void should_ThrowException_when_TrackNotFound() {
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ReorderPlaylistTrackRequest(UUID.randomUUID(), trackId, null, null)));
    }

    @Test
    void should_ThrowException_when_TrackBelongsToDifferentPlaylist() {
        UUID playlistId = UUID.randomUUID();
        PlaylistTrack track = trackIn(UUID.randomUUID()); // other playlist
        when(playlistTrackRepository.findById(track.getId())).thenReturn(Optional.of(track));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ReorderPlaylistTrackRequest(playlistId, track.getId(), null, null)));
    }

    @Test
    void should_RecalculateRank_and_PublishEvent_when_TrackInPlaylist() {
        UUID playlistId = UUID.randomUUID();
        PlaylistTrack track = trackIn(playlistId);
        when(playlistTrackRepository.findById(track.getId())).thenReturn(Optional.of(track));

        useCase.execute(new ReorderPlaylistTrackRequest(playlistId, track.getId(), "a", "c"));

        assertEquals(new LexoRank("b"), track.getLexoRank());
        verify(playlistTrackRepository).save(track);
        ArgumentCaptor<TrackMovedInPlaylist> eventCaptor = ArgumentCaptor.forClass(TrackMovedInPlaylist.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals(track.getPlaylistId(), eventCaptor.getValue().getPlaylistId());
        assertEquals(track.getTrackId(), eventCaptor.getValue().getTrackId());
        assertEquals("b", eventCaptor.getValue().getNewRank());
    }

    @Test
    void should_TriggerRebalance_when_NewRankExceedsPrecisionLimit() {
        UUID playlistId = UUID.randomUUID();
        PlaylistTrack track = trackIn(playlistId);
        when(playlistTrackRepository.findById(track.getId())).thenReturn(Optional.of(track));
        LexoRank longRank = new LexoRank("a".repeat(31));
        when(lexoRankService.calculateMid(any(), any())).thenReturn(longRank);

        useCase.execute(new ReorderPlaylistTrackRequest(playlistId, track.getId(), "a", "b"));

        verify(rebalancePlaylistUseCase).execute(playlistId);
    }

    @Test
    void should_NotTriggerRebalance_when_NewRankIsShort() {
        UUID playlistId = UUID.randomUUID();
        PlaylistTrack track = trackIn(playlistId);
        when(playlistTrackRepository.findById(track.getId())).thenReturn(Optional.of(track));

        useCase.execute(new ReorderPlaylistTrackRequest(playlistId, track.getId(), "a", "c"));

        verify(rebalancePlaylistUseCase, never()).execute(any());
    }
}