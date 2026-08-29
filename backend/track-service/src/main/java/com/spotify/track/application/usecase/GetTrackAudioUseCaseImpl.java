package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.GetTrackAudioCommand;
import com.spotify.track.domain.entity.TrackAudioRange;
import com.spotify.track.domain.exception.TrackAudioNotFoundException;
import com.spotify.track.domain.repository.TrackAudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTrackAudioUseCaseImpl implements GetTrackAudioUseCase {

    private final TrackAudioRepository trackAudioRepository;

    @Override
    @Transactional(readOnly = true)
    public TrackAudioRange execute(GetTrackAudioCommand command) {
        return trackAudioRepository.getAudio(command.trackId(), command.offset(), command.length());
    }
}