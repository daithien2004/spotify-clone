package com.spotify.search.infrastructure.bootstrap;

import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reindexes existing tracks on startup so the index is not empty when Elasticsearch comes
 * up after the catalog was seeded. Elasticsearch unavailability must NOT crash the app —
 * runtime Kafka events backfill, and the bootstrap logs the failure instead (spec §6.4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrackIndexBootstrap implements ApplicationRunner {

    private final TrackBootstrapFetcher fetcher;
    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // ES index may not exist yet on a fresh box — ensure it before the first index/search,
            // otherwise both throw index_not_found. Idempotent (checks existence first).
            trackSearchRepository.ensureIndex();
            var tracks = fetcher.fetchAll();
            log.info("Bootstrap: indexing {} tracks from track-service", tracks.size());
            for (TrackPayload payload : tracks) {
                trackSearchRepository.index(toDocument(payload));
            }
        } catch (Exception e) {
            log.error("Bootstrap reindex skipped (indexing will catch up via Kafka events): {}", e.getMessage());
        }
    }

    private TrackSearchDocument toDocument(TrackPayload payload) {
        return new TrackSearchDocument(
                UUID.fromString(payload.id()),
                payload.title(),
                payload.artist(),
                payload.album(),
                payload.durationMs(),
                payload.artworkUrl(),
                payload.audioUrl());
    }
}