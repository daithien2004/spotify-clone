package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import java.util.List;

/** Command handler: full-text search over indexed tracks. */
public interface SearchTracksUseCase {
    List<SearchTrackItem> execute(SearchTracksCommand command);
}