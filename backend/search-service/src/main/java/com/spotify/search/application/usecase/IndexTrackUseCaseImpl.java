package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexTrackUseCaseImpl implements IndexTrackUseCase {

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void execute(IndexTrackCommand command) {
        TrackSearchDocument document = command.document();
        if (document == null || document.id() == null) {
            throw new IllegalArgumentException("track document with id is required");
        }
        trackSearchRepository.index(document);
    }
}