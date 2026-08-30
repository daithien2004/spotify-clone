package com.spotify.search.infrastructure.bootstrap;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;
import com.spotify.common.infrastructure.web.ApiResponse;

/** HTTP adapter over {@code GET {track-service}/api/v1/tracks} (list-all serializes the catalog). */
@Component
public class RestTrackBootstrapFetcher implements TrackBootstrapFetcher {

    private static final ParameterizedTypeReference<ApiResponse<List<TrackPayload>>> RESPONSE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public RestTrackBootstrapFetcher(
            @Value("${search.track-service.base-url:http://localhost:8085}") String trackServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(trackServiceBaseUrl).build();
    }

    @Override
    public List<TrackPayload> fetchAll() {
        ApiResponse<List<TrackPayload>> response = restClient.get()
                .uri("/api/v1/tracks")
                .retrieve()
                .body(RESPONSE);
        return response != null && response.data() != null ? response.data() : List.of();
    }
}