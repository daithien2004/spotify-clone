package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.UploadTrackAudioCommand;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.DomainEventPublisher;
import com.spotify.track.domain.repository.TrackAudioRepository;
import com.spotify.track.domain.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadTrackAudioUseCaseImpl implements UploadTrackAudioUseCase {

    private final TrackRepository trackRepository;
    private final TrackAudioRepository trackAudioRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(UploadTrackAudioCommand command) {
        Track track = trackRepository.findById(command.trackId())
                .orElseThrow(() -> new TrackNotFoundException(command.trackId()));

        validate(command);

        trackAudioRepository.putAudio(track.getId(), command.content(), command.size(), command.contentType());

        // Canonical relative stream path — FE prefixes its gateway base URL
        String streamPath = "/api/v1/tracks/" + track.getId() + "/audio";
        trackRepository.save(track.withAudioUrl(streamPath));

        eventPublisher.publish(new TrackAudioUploaded(track.getId()));
    }

    private void validate(UploadTrackAudioCommand command) {
        if (command.size() <= 0) {
            throw new IllegalArgumentException("Audio file is empty");
        }
        if (command.size() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Audio file exceeds " + (MAX_AUDIO_BYTES / (1024 * 1024)) + " MB limit");
        }
    }
}