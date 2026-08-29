package com.spotify.playlist.presentation.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.playlist.application.dto.AddTrackToPlaylistRequest;
import com.spotify.playlist.application.dto.PlaylistTrackResponse;
import com.spotify.playlist.application.dto.ReorderPlaylistTrackRequest;
import com.spotify.playlist.application.usecase.AddTrackToPlaylistUseCase;
import com.spotify.playlist.application.usecase.GetPlaylistTracksUseCase;
import com.spotify.playlist.application.usecase.ReorderPlaylistTrackUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistTrackController {

    private final ReorderPlaylistTrackUseCase reorderPlaylistTrackUseCase;
    private final AddTrackToPlaylistUseCase addTrackToPlaylistUseCase;
    private final GetPlaylistTracksUseCase getPlaylistTracksUseCase;

    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistTrackResponse> addTrack(
            @PathVariable UUID playlistId,
            @RequestBody @Valid AddTrackToPlaylistRequest request) {
        // Path playlistId is authoritative; request body supplies only the trackId
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PlaylistTrackResponse.from(
                        addTrackToPlaylistUseCase.execute(new AddTrackToPlaylistRequest(playlistId, request.trackId()))));
    }

    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<List<PlaylistTrackResponse>> getTracks(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(getPlaylistTracksUseCase.execute(playlistId));
    }

    @PutMapping("/{playlistId}/tracks/{playlistTrackId}/reorder")
    public ResponseEntity<Void> reorderTrack(
            @PathVariable UUID playlistId,
            @PathVariable UUID playlistTrackId,
            @RequestBody ReorderPlaylistTrackRequest request) {
        // Path ids are authoritative; request body carries the prev/next rank boundaries
        reorderPlaylistTrackUseCase.execute(new ReorderPlaylistTrackRequest(
                playlistId,
                playlistTrackId,
                request.prevRank(),
                request.nextRank()));

        return ResponseEntity.noContent().build();
    }
}