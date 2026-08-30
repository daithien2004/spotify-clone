package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.domain.repository.TrackSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchTracksUseCaseImpl implements SearchTracksUseCase {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public List<SearchTrackItem> execute(SearchTracksCommand command) {
        if (command.query() == null || command.query().isBlank()) {
            throw new IllegalArgumentException("q is required");
        }
        int limit = Math.max(MIN_LIMIT, Math.min(command.limit(), MAX_LIMIT));
        // Normalize so the adapter always receives a canonical query; ES owns relevance ranking.
        String query = command.query().trim().toLowerCase();
        return trackSearchRepository.search(query, limit).stream()
                .map(SearchTrackItem::from)
                .toList();
    }
}