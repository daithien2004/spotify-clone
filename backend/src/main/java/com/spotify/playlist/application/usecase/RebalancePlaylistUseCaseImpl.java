package com.spotify.playlist.application.usecase;

import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.valueobject.LexoRank;
import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;
import com.spotify.playlist.infrastructure.persistence.repository.JpaPlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RebalancePlaylistUseCaseImpl implements RebalancePlaylistUseCase {

    private final JpaPlaylistTrackRepository jpaRepository; // Direct JPA for batch update efficiency

    @Async
    @Override
    @Transactional
    public void execute(UUID playlistId) {
        log.info("Starting rebalance for playlist {}", playlistId);

        // 1. Fetch all tracks of this playlist sorted by current rank
        // Note: For real scale, use pagination or specialized JPA query
        List<PlaylistTrackJpaEntity> tracks = jpaRepository.findAll(Sort.by(Sort.Direction.ASC, "lexoRank"));

        // 2. Re-assign ranks with large gaps
        // We'll use a simple numeric-based gap logic for rebalancing: "1000", "2000", "3000"...
        for (int i = 0; i < tracks.size(); i++) {
            String newRank = String.format("%08d", (i + 1) * 1000);
            tracks.get(i).setLexoRank(newRank);
        }

        // 3. Batch save
        jpaRepository.saveAll(tracks);

        log.info("Finished rebalancing {} tracks for playlist {}", tracks.size(), playlistId);
    }
}
