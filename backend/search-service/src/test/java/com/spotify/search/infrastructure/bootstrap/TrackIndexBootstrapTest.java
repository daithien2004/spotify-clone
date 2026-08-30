package com.spotify.search.infrastructure.bootstrap;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackIndexBootstrapTest {

    @Mock
    private TrackBootstrapFetcher fetcher;
    @Mock
    private TrackSearchRepository repository;

    private TrackIndexBootstrap bootstrap;

    @Test
    void should_IndexAllTracks_when_FetcherReturnsData() {
        TrackEventEnvelope.TrackPayload a = new TrackEventEnvelope.TrackPayload("9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b",
                "Free Spirit", "Khalid", "Free Spirit (Explicit)", 182_000L, "https://a.png", null);
        TrackEventEnvelope.TrackPayload b = new TrackEventEnvelope.TrackPayload("7b3c9f2e-4d51-4b20-a5ce-10f8d97c4e5f",
                "Ocean Front Apt.", "ayokay", "Digital Dreamscape", 132_000L, "https://b.png", null);
        when(fetcher.fetchAll()).thenReturn(List.of(a, b));
        bootstrap = new TrackIndexBootstrap(fetcher, repository);

        bootstrap.run(null);

        verify(repository, times(2)).index(any(TrackSearchDocument.class));
    }

    @Test
    void should_NotCrash_when_TrackServiceUnreachable() {
        doThrow(new RuntimeException("connection refused")).when(fetcher).fetchAll();
        bootstrap = new TrackIndexBootstrap(fetcher, repository);

        assertDoesNotThrow(() -> bootstrap.run(null));

        verify(repository, times(0)).index(any(TrackSearchDocument.class));
    }
}