package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.domain.exception.PlaylistNotFoundException;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPlaylistByIdUseCaseImpl implements GetPlaylistByIdUseCase {

    private final PlaylistRepository playlistRepository;

    @Override
    @Transactional(readOnly = true)
    public PlaylistResponse execute(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
        return PlaylistResponse.from(playlist);
    }
}