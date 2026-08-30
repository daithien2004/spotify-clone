package com.spotify.search.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.usecase.SearchTracksUseCase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/** Search API — {@code GET /api/v1/search/tracks?q=&limit=} (spec §4). */
@Validated
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchTracksUseCase searchTracksUseCase;

    @GetMapping("/tracks")
    public ResponseEntity<List<SearchTrackItem>> searchTracks(
            @RequestParam("q") @NotBlank String q,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(searchTracksUseCase.execute(new SearchTracksCommand(q, limit)));
    }
}