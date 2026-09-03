package com.spotify.playlist.presentation.controller;

import com.spotify.playlist.application.dto.CreatePlaylistRequest;
import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.application.dto.PlaylistSummaryResponse;
import com.spotify.playlist.application.usecase.CreatePlaylistUseCase;
import com.spotify.playlist.application.usecase.GetPlaylistByIdUseCase;
import com.spotify.playlist.application.usecase.ListPlaylistsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final CreatePlaylistUseCase createPlaylistUseCase;

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(getPlaylistByIdUseCase.execute(playlistId));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistSummaryResponse>> listPlaylists() {
        return ResponseEntity.ok(listPlaylistsUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> createPlaylist(
            HttpServletRequest request,
            @RequestBody @Valid CreatePlaylistRequest createRequest) {
        // Authenticated via gateway JWT filter → read owner from X-User-Id header
        // (same idiom as auth-service /me). The header only exists after gateway auth,
        // and ServiceSecurityConfig guards /api/v1/playlists/** as authenticated.
        UUID ownerUserId = UUID.fromString(request.getHeader("X-User-Id"));
        return ResponseEntity.status(HttpStatus.CREATED).body(createPlaylistUseCase.execute(
                new CreatePlaylistUseCase.CreatePlaylistCommand(ownerUserId, createRequest.title(), createRequest.description())));
    }
}
