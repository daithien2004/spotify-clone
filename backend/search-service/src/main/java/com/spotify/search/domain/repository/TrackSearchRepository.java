package com.spotify.search.domain.repository;

import com.spotify.search.domain.entity.TrackSearchDocument;
import java.util.List;
import java.util.UUID;

/** Port into the Elasticsearch index (spec §3). */
public interface TrackSearchRepository {
    /** Creates the index with the spec mapping when missing — idempotent, safe to call on startup. */
    void ensureIndex();

    void index(TrackSearchDocument document);

    void remove(UUID trackId);

    List<TrackSearchDocument> search(String query, int limit);
}
