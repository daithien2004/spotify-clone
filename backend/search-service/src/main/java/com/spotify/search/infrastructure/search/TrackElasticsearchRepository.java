package com.spotify.search.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import com.spotify.search.infrastructure.config.ElasticsearchConfig;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Elasticsearch adapter over the `tracks` index (spec §5). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackElasticsearchRepository implements TrackSearchRepository {

    private final ElasticsearchClient client;

    @Override
    public void ensureIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(ElasticsearchConfig.TRACK_INDEX)).value();
            if (!exists) {
                client.indices().create(c -> c.index(ElasticsearchConfig.TRACK_INDEX)
                        .withJson(new StringReader(ElasticsearchConfig.TRACK_INDEX_MAPPING)));
                log.info("Created Elasticsearch index '{}'", ElasticsearchConfig.TRACK_INDEX);
            }
        } catch (IOException | ElasticsearchException e) {
            log.error("Could not ensure index '{}' — search may be unavailable",
                    ElasticsearchConfig.TRACK_INDEX, e);
        }
    }

    @Override
    public void index(TrackSearchDocument document) {
        try {
            client.index(i -> i.index(ElasticsearchConfig.TRACK_INDEX)
                    .id(document.id().toString())
                    .document(document));
        } catch (IOException e) {
            throw new RuntimeException("Failed to index track " + document.id(), e);
        }
    }

    @Override
    public void remove(UUID trackId) {
        try {
            client.delete(d -> d.index(ElasticsearchConfig.TRACK_INDEX).id(trackId.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove track " + trackId + " from search index", e);
        }
    }

    @Override
    public List<TrackSearchDocument> search(String query, int limit) {
        try {
            SearchResponse<TrackSearchDocument> response = client.search(
                    s -> s.index(ElasticsearchConfig.TRACK_INDEX)
                            .size(limit)
                            .query(q -> q.multiMatch(m -> m
                                    .query(query)
                                    .fields(List.of("title^3", "artist^2", "album"))
                                    .fuzziness("AUTO"))),
                    TrackSearchDocument.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }
}