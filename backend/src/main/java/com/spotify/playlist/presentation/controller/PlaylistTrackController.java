package com.spotify.playlist.presentation.controller;

import org.hibernate.validator.constraints.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.playlist.application.dto.ReorderPlaylistTrackRequest;
import com.spotify.playlist.application.usecase.ReorderPlaylistTrackUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistTrackController {

    private final ReorderPlaylistTrackUseCase reorderPlaylistTrackUseCase;

    @PutMapping("/{playlistId}/tracks/{playlistTrackId}/reorder")
    public ResponseEntity<Void> reorderTrack(
            @PathVariable UUID playlistId,
            @PathVariable UUID playlistTrackId,
            @RequestBody ReorderPlaylistTrackRequest request) {
        // Enforce the path variables if needed, though they are usually redundant if
        // DTO already has them
        // For consistency with the spec/design, we process the request
        reorderPlaylistTrackUseCase.execute(new ReorderPlaylistTrackRequest(
                playlistTrackId,
                request.prevRank(),
                request.nextRank()));

        return ResponseEntity.noContent().build();
    }
}
