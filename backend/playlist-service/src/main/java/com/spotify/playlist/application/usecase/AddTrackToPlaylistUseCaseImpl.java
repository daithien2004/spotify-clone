package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.AddTrackToPlaylistRequest;
import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.event.TrackAddedInPlaylist;
import com.spotify.playlist.domain.repository.DomainEventPublisher;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.service.LexoRankService;
import com.spotify.playlist.domain.valueobject.LexoRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddTrackToPlaylistUseCaseImpl implements AddTrackToPlaylistUseCase {

    private static final LexoRank END_OF_RANGE = new LexoRank("z");

    private final PlaylistTrackRepository playlistTrackRepository;
    private final LexoRankService lexoRankService;
    private final DomainEventPublisher domainEventPublisher;
    private final RebalancePlaylistUseCase rebalancePlaylistUseCase;

    @Override
    @Transactional
    public PlaylistTrack execute(AddTrackToPlaylistRequest request) {
        List<PlaylistTrack> existing = playlistTrackRepository.findAllByPlaylistId(request.playlistId());

        // Empty playlist starts at the initial rank; otherwise append after the current last track
        LexoRank newRank = existing.isEmpty()
                ? lexoRankService.getInitialRank()
                : lexoRankService.calculateMid(existing.get(existing.size() - 1).getLexoRank(), END_OF_RANGE);

        // Appends eventually starve toward the end of the alphabet — restore headroom early
        if (newRank.value().length() > LexoRankService.PRECISION_LIMIT) {
            log.warn("LexoRank precision limit reached for playlist {}. Rebalancing triggered.", request.playlistId());
            rebalancePlaylistUseCase.execute(request.playlistId());
        }

        PlaylistTrack track = PlaylistTrack.builder()
                .id(UUID.randomUUID())
                .playlistId(request.playlistId())
                .trackId(request.trackId())
                .lexoRank(newRank)
                .addedAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        playlistTrackRepository.save(track);
        domainEventPublisher.publish(new TrackAddedInPlaylist(request.playlistId(), request.trackId()));
        return track;
    }
}