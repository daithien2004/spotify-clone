package com.spotify.track.presentation.controller;

import com.spotify.track.application.dto.GetTrackAudioCommand;
import com.spotify.track.application.dto.UploadTrackAudioCommand;
import com.spotify.track.application.usecase.GetTrackAudioUseCase;
import com.spotify.track.application.usecase.UploadTrackAudioUseCase;
import com.spotify.track.domain.entity.TrackAudioRange;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Audio upload + streaming. The GET endpoint supports HTTP byte ranges so
 * browsers can seek inside a track. Protected by the gateway JWT filter.
 */
@RestController
@RequestMapping("/api/v1/tracks/{trackId}/audio")
@RequiredArgsConstructor
public class TrackAudioController {

    private final UploadTrackAudioUseCase uploadTrackAudioUseCase;
    private final GetTrackAudioUseCase getTrackAudioUseCase;

    @PutMapping
    public ResponseEntity<Void> uploadAudio(
            @PathVariable UUID trackId,
            @RequestParam("file") MultipartFile file) throws IOException {
        try (var content = file.getInputStream()) {
            uploadTrackAudioUseCase.execute(new UploadTrackAudioCommand(
                    trackId, content, file.getSize(),
                    file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<org.springframework.core.io.InputStreamResource> streamAudio(
            @PathVariable UUID trackId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        long offset = 0;
        long length = Long.MAX_VALUE;

        // Parse single byte range (bytes=start-end); fall back to full content otherwise.
        if (rangeHeader != null && !rangeHeader.isBlank()) {
            HttpRange range = firstSingleRange(rangeHeader);
            if (range != null) {
                offset = range.getRangeStart(0);
                length = range.getRangeEnd(0) - offset + 1;
            }
        }

        TrackAudioRange audio = getTrackAudioUseCase.execute(new GetTrackAudioCommand(trackId, offset, length));

        var resource = new org.springframework.core.io.InputStreamResource(audio.content());
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, audio.contentType())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + audio.offset() + "-" + (audio.offset() + audio.length() - 1) + "/" + audio.totalSize())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(audio.length()))
                .body(resource);
    }

    /** Returns the first well-formed byte range, or null if none parse. */
    private HttpRange firstSingleRange(String rangeHeader) {
        for (HttpRange range : HttpRange.parseRanges(rangeHeader)) {
            // Invalid "bytes=N-" style ranges yield Long.MIN_VALUE for the start
            if (range.getRangeStart(0) != Long.MIN_VALUE) {
                return range;
            }
        }
        return null;
    }
}