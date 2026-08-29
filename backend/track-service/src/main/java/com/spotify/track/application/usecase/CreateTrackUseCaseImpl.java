package com.spotify.track.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.event.TrackUploaded;
import com.spotify.track.domain.repository.DomainEventPublisher;
import com.spotify.track.domain.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTrackUseCaseImpl implements CreateTrackUseCase {

    private final TrackRepository trackRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public Track execute(CreateTrackRequest request) {
        validate(request);

        Track track = Track.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .artist(request.artist())
                .album(request.album())
                .durationMs(request.durationMs())
                .artworkUrl(request.artworkUrl())
                .audioUrl(null) // filled by the MinIO upload phase
                .build();

        Track saved = trackRepository.save(track);
        domainEventPublisher.publish(new TrackUploaded(
                saved.getId(), saved.getTitle(), saved.getArtist(), saved.getAlbum(),
                saved.getDurationMs(), saved.getArtworkUrl(), saved.getAudioUrl()));
        return saved;
    }

    private void validate(CreateTrackRequest request) {
        // Guards mirror the DTO constraints so the use case stays correct without the web layer
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