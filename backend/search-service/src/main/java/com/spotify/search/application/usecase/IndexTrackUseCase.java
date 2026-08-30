package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.IndexTrackCommand;

/** Command handler: index one track document into Elasticsearch. */
public interface IndexTrackUseCase {
    void execute(IndexTrackCommand command);
}