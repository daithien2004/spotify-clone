package com.spotify.search.infrastructure.config;

/**
 * Index name + mapping for the search index. The mapping mirrors the spec §5 contract
 * (artwork/audio URLs stored but not analyzed; duration is a numeric filterable field).
 */
public final class ElasticsearchConfig {
    public static final String TRACK_INDEX = "tracks";

    public static final String TRACK_INDEX_MAPPING = """
            {
              "mappings": {
                "properties": {
                  "id": {"type": "keyword"},
                  "title": {"type": "text"},
                  "artist": {"type": "text"},
                  "album": {"type": "text"},
                  "artworkUrl": {"type": "keyword", "index": false},
                  "audioUrl": {"type": "keyword", "index": false},
                  "durationMs": {"type": "long"}
                }
              }
            }
            """;

    private ElasticsearchConfig() {
        // constants holder
    }
}