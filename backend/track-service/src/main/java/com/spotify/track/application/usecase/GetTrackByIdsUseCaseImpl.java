package com.spotify.track.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetTrackByIdsUseCaseImpl implements GetTrackByIdsUseCase {

    private final TrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> execute(List<UUID> trackIds) {
        if (trackIds.isEmpty()) {
            return List.of();
        }

        List<Track> found = trackRepository.findAllByIds(trackIds);

        // Index by id so the response can be rebuilt in the exact requested order
        Map<UUID, Track> tracksById = found.stream()
                .collect(Collectors.toMap(Track::getId, Function.identity()));

        for (UUID trackId : trackIds) {
            if (!tracksById.containsKey(trackId)) {
                throw new TrackNotFoundException(trackId);
            }
        }

        return trackIds.stream()
                .map(tracksById::get)
                .map(TrackResponse::from)
                .toList();
    }
}