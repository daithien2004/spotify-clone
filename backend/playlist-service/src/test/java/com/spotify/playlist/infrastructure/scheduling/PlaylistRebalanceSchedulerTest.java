package com.spotify.playlist.infrastructure.scheduling;

import com.spotify.playlist.application.usecase.RebalancePlaylistUseCase;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.service.LexoRankService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistRebalanceSchedulerTest {

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;
    @Mock
    private RebalancePlaylistUseCase rebalancePlaylistUseCase;

    @InjectMocks
    private PlaylistRebalanceScheduler scheduler;

    @Test
    void should_RebalanceEveryOverloadedPlaylist() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(playlistTrackRepository.findPlaylistIdsNeedingRebalance(LexoRankService.PRECISION_LIMIT))
                .thenReturn(List.of(id1, id2));

        scheduler.rebalanceOverloadedPlaylists();

        verify(rebalancePlaylistUseCase).execute(id1);
        verify(rebalancePlaylistUseCase).execute(id2);
    }

    @Test
    void should_DoNothing_when_NoPlaylistIsOverloaded() {
        when(playlistTrackRepository.findPlaylistIdsNeedingRebalance(LexoRankService.PRECISION_LIMIT))
                .thenReturn(List.of());

        scheduler.rebalanceOverloadedPlaylists();

        verify(rebalancePlaylistUseCase, never()).execute(any());
    }
}