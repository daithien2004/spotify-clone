package com.spotify.playlist.presentation.controller;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.application.dto.PlaylistSummaryResponse;
import com.spotify.playlist.application.usecase.GetPlaylistByIdUseCase;
import com.spotify.playlist.application.usecase.ListPlaylistsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final GetPlaylistByIdUseCase getPlaylistByIdUseCase;
    private final ListPlaylistsUseCase listPlaylistsUseCase;

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(getPlaylistByIdUseCase.execute(playlistId));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistSummaryResponse>> listPlaylists() {
        return ResponseEntity.ok(listPlaylistsUseCase.execute());
    }
}