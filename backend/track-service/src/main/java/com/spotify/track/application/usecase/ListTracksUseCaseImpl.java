package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.repository.TrackRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListTracksUseCaseImpl implements ListTracksUseCase {

    private final TrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> execute() {
        return trackRepository.findAll().stream()
                .map(TrackResponse::from)
                .toList();
    }
}
