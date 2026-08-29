package com.spotify.playlist.application.usecase;

import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.valueobject.LexoRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RebalancePlaylistUseCaseImpl implements RebalancePlaylistUseCase {

    private final PlaylistTrackRepository playlistTrackRepository;

    @Async
    @Override
    @Transactional
    public void execute(UUID playlistId) {
        log.info("Starting rebalance for playlist {}", playlistId);

        // Only this playlist's tracks are touched — never the whole table
        List<PlaylistTrack> tracks = playlistTrackRepository.findAllByPlaylistId(playlistId);

        // Re-assign evenly spaced ranks with large gaps to restore precision headroom
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).updateRank(new LexoRank(String.format("%08d", (i + 1) * 1000)));
        }

        playlistTrackRepository.saveAll(tracks);

        log.info("Finished rebalancing {} tracks for playlist {}", tracks.size(), playlistId);
    }
}