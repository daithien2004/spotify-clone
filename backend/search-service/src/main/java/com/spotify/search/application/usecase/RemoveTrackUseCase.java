package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.RemoveTrackCommand;

/** Command handler: remove one track document from Elasticsearch. */
public interface RemoveTrackUseCase {
    void execute(RemoveTrackCommand command);
}