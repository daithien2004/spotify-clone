package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistTrackResponse;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPlaylistTracksUseCaseImpl implements GetPlaylistTracksUseCase {

    private final PlaylistTrackRepository playlistTrackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistTrackResponse> execute(UUID playlistId) {
        // Repository returns tracks ordered by lexo rank (see JpaPlaylistTrackRepository)
        return playlistTrackRepository.findAllByPlaylistId(playlistId).stream()
                .map(PlaylistTrackResponse::from)
                .toList();
    }
}