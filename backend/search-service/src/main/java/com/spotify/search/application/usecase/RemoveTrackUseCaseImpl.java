package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.domain.repository.TrackSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveTrackUseCaseImpl implements RemoveTrackUseCase {

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void execute(RemoveTrackCommand command) {
        if (command.trackId() == null) {
            throw new IllegalArgumentException("trackId is required");
        }
        trackSearchRepository.remove(command.trackId());
    }
}