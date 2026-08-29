package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistSummaryResponse;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPlaylistsUseCaseImpl implements ListPlaylistsUseCase {

    private final PlaylistRepository playlistRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> execute() {
        return playlistRepository.findAll().stream()
                .map(PlaylistSummaryResponse::from)
                .toList();
    }
}