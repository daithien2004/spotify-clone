package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.AddTrackToPlaylistRequest;
import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.event.TrackAddedInPlaylist;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddTrackToPlaylistUseCaseImplTest {

    @Spy
    private final LexoRankService lexoRankService = new LexoRankService();

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    @Mock
    private RebalancePlaylistUseCase rebalancePlaylistUseCase;

    @InjectMocks
    private AddTrackToPlaylistUseCaseImpl useCase;

    private PlaylistTrack trackOf(String rank) {
        return PlaylistTrack.builder()
                .id(UUID.randomUUID())
                .playlistId(UUID.randomUUID())
                .trackId(UUID.randomUUID())
                .lexoRank(new LexoRank(rank))
                .addedAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void should_AssignInitialRank_when_PlaylistEmpty() {
        UUID playlistId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of());

        PlaylistTrack saved = useCase.execute(new AddTrackToPlaylistRequest(playlistId, trackId));

        assertNotNull(saved.getId());
        assertEquals(trackId, saved.getTrackId());
        assertEquals(playlistId, saved.getPlaylistId());
        assertEquals("m", saved.getLexoRank().value()); // LexoRankService.getInitialRank()
        verify(playlistTrackRepository).save(saved);
        verify(domainEventPublisher).publish(org.mockito.ArgumentMatchers.isA(TrackAddedInPlaylist.class));
    }

    @Test
    void should_AppendAtEnd_when_PlaylistHasTracks() {
        UUID playlistId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId))
                .thenReturn(List.of(trackOf("m"), trackOf("t")));

        PlaylistTrack saved = useCase.execute(new AddTrackToPlaylistRequest(playlistId, trackId));

        // midpoint between last rank "t" and end-of-range "z"
        assertEquals(new LexoRank("w"), saved.getLexoRank());
    }

    @Test
    void should_PublishTrackAddedEvent_with_PlaylistAndTrack() {
        UUID playlistId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of());

        useCase.execute(new AddTrackToPlaylistRequest(playlistId, trackId));

        ArgumentCaptor<TrackAddedInPlaylist> captor = ArgumentCaptor.forClass(TrackAddedInPlaylist.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertEquals(playlistId, captor.getValue().getPlaylistId());
        assertEquals(trackId, captor.getValue().getTrackId());
    }

    @Test
    void should_TriggerRebalance_when_NewRankExceedsPrecisionLimit() {
        UUID playlistId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of(trackOf("m")));
        LexoRank longRank = new LexoRank("a".repeat(31));
        when(lexoRankService.calculateMid(any(), any())).thenReturn(longRank);

        useCase.execute(new AddTrackToPlaylistRequest(playlistId, trackId));

        verify(rebalancePlaylistUseCase).execute(playlistId);
    }

    @Test
    void should_NotTriggerRebalance_when_NewRankIsShort() {
        UUID playlistId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of(trackOf("m")));

        useCase.execute(new AddTrackToPlaylistRequest(playlistId, trackId));

        verify(rebalancePlaylistUseCase, never()).execute(any());
    }
}