package com.spotify.track.presentation.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.application.usecase.CreateTrackUseCase;
import com.spotify.track.application.usecase.GetTrackByIdsUseCase;
import com.spotify.track.application.usecase.ListTracksUseCase;
import com.spotify.track.application.usecase.UpdateTrackUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final CreateTrackUseCase createTrackUseCase;
    private final GetTrackByIdsUseCase getTrackByIdsUseCase;
    private final ListTracksUseCase listTracksUseCase;
    private final UpdateTrackUseCase updateTrackUseCase;

    @PostMapping
    public ResponseEntity<TrackResponse> createTrack(@RequestBody @Valid CreateTrackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrackResponse.from(createTrackUseCase.execute(request)));
    }

    /** Single metadata lookup — the batch read keeps input order, so take the single element. */
    @GetMapping("/{trackId}")
    public ResponseEntity<TrackResponse> getTrack(@PathVariable UUID trackId) {
        return ResponseEntity.ok(getTrackByIdsUseCase.execute(List.of(trackId)).getFirst());
    }

    /** Batch lookup for playlist joins, or full catalog when ids is omitted for bootstrap. */
    @GetMapping
    public ResponseEntity<List<TrackResponse>> getTracks(
            @RequestParam(value = "ids", required = false) List<UUID> ids) {
        if (ids == null) {
            return ResponseEntity.ok(listTracksUseCase.execute());
        }
        return ResponseEntity.ok(getTrackByIdsUseCase.execute(ids));
    }

    @PutMapping("/{trackId}")
    public ResponseEntity<TrackResponse> updateTrack(
            @PathVariable UUID trackId,
            @RequestBody @Valid CreateTrackRequest request) {
        return ResponseEntity.ok(updateTrackUseCase.execute(trackId, request));
    }
}