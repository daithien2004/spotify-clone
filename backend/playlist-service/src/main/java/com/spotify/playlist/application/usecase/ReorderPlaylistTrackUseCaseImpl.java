package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.ReorderPlaylistTrackRequest;
import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.event.TrackMovedInPlaylist;
import com.spotify.playlist.domain.repository.DomainEventPublisher;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.service.LexoRankService;
import com.spotify.playlist.domain.valueobject.LexoRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReorderPlaylistTrackUseCaseImpl implements ReorderPlaylistTrackUseCase {

    private final PlaylistTrackRepository playlistTrackRepository;
    private final LexoRankService lexoRankService;
    private final RebalancePlaylistUseCase rebalancePlaylistUseCase;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public void execute(ReorderPlaylistTrackRequest request) {
        PlaylistTrack track = playlistTrackRepository.findById(request.playlistTrackId())
                .orElseThrow(() -> new IllegalArgumentException("Playlist track not found"));

        if (!track.getPlaylistId().equals(request.playlistId())) {
            throw new IllegalArgumentException("Playlist track does not belong to playlist " + request.playlistId());
        }

        LexoRank prev = request.prevRank() != null ? new LexoRank(request.prevRank()) : null;
        LexoRank next = request.nextRank() != null ? new LexoRank(request.nextRank()) : null;

        LexoRank newRank = lexoRankService.calculateMid(prev, next);
        
        track.updateRank(newRank);
        playlistTrackRepository.save(track);
        
        domainEventPublisher.publish(new TrackMovedInPlaylist(track.getPlaylistId(), track.getTrackId(), newRank.value()));

        // Precision check — delegate heavy work to the async rebalance use case
        if (newRank.value().length() > LexoRankService.PRECISION_LIMIT) {
            log.warn("LexoRank precision limit reached for playlist {}. Rebalancing triggered.", track.getPlaylistId());
            rebalancePlaylistUseCase.execute(track.getPlaylistId());
        }
    }
}
