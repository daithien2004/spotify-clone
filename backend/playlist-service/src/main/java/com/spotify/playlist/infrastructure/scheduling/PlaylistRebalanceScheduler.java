package com.spotify.playlist.infrastructure.scheduling;

import com.spotify.playlist.application.usecase.RebalancePlaylistUseCase;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.service.LexoRankService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Periodic sweep that restores rank-precision headroom for playlists whose ranks
 * have grown overloaded. Each playlist is rebalanced async on the bounded executor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistRebalanceScheduler {

    private final PlaylistTrackRepository playlistTrackRepository;
    private final RebalancePlaylistUseCase rebalancePlaylistUseCase;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void scheduledRebalance() {
        rebalanceOverloadedPlaylists();
    }

    void rebalanceOverloadedPlaylists() {
        List<UUID> playlistIds = playlistTrackRepository.findPlaylistIdsNeedingRebalance(LexoRankService.PRECISION_LIMIT);
        if (playlistIds.isEmpty()) {
            return;
        }
        log.info("Scheduled rebalance for {} overloaded playlists", playlistIds.size());
        playlistIds.forEach(rebalancePlaylistUseCase::execute);
    }
}