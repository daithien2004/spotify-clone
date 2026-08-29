package com.spotify.track.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.event.TrackUpdated;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.DomainEventPublisher;
import com.spotify.track.domain.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateTrackUseCaseImpl implements UpdateTrackUseCase {

    private final TrackRepository trackRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public TrackResponse execute(UUID trackId, CreateTrackRequest request) {
        Track existing = trackRepository.findById(trackId)
                .orElseThrow(() -> new TrackNotFoundException(trackId));

        validate(request);

        Track updated = existing.withUpdatedMetadata(
                request.title(),
                request.artist(),
                request.album(),
                request.durationMs(),
                request.artworkUrl());

        Track saved = trackRepository.save(updated);
        domainEventPublisher.publish(new TrackUpdated(
                saved.getId(), saved.getTitle(), saved.getArtist(), saved.getAlbum(),
                saved.getDurationMs(), saved.getArtworkUrl(), saved.getAudioUrl()));
        return TrackResponse.from(saved);
    }

    private void validate(CreateTrackRequest request) {
        if (request.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.artist().isBlank()) {
            throw new IllegalArgumentException("artist is required");
        }
        if (request.durationMs() == null || request.durationMs() < 0) {
            throw new IllegalArgumentException("durationMs must be >= 0");
        }
    }
}