package com.spotify.track.presentation.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.application.usecase.CreateTrackUseCase;
import com.spotify.track.application.usecase.GetTrackByIdsUseCase;
import com.spotify.track.application.usecase.ListTracksUseCase;
import com.spotify.track.application.usecase.UpdateTrackUseCase;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.presentation.controller.TrackController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P3 — Contract test phía PROVIDER (track-service).
 *
 * <p>Xác minh {@code GET /api/v1/tracks} (list-all, bootstrap reindex search-service) vẫn
 * trả đúng contract JSON mà consumer (search-service {@code RestTrackBootstrapFetcher}) phụ
 * thuộc, theo fixture chung {repo}/backend/contracts/track-list-all.contract.json. So sánh
 * mảng {@code data} (các field track) với fixture, bỏ qua {@code timestamp} volatile.
 *
 * <p>Lý do tồn tại: consumer parse từng phần tử thành {@code TrackPayload} (7 field, id
 * String) trong khi producer trả {@code TrackResponse} (9 field, id UUID). Hiện chỉ chạy
 * được nhờ Jackson lenient bỏ qua field thừa — nếu sau này producer bỏ field mà consumer
 * cần (vd `title`), bootstrap sẽ âm thầm index dữ liệu rỗng. Test này chốt contract 2 chiều.
 */
@WebMvcTest(TrackController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackListAllContractProviderTest {

    private static final Path CONTRACT =
            Path.of("..", "contracts", "track-list-all.contract.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private CreateTrackUseCase createTrackUseCase;
    @MockBean private GetTrackByIdsUseCase getTrackByIdsUseCase;
    @MockBean private ListTracksUseCase listTracksUseCase;
    @MockBean private UpdateTrackUseCase updateTrackUseCase;

    private Track track(String id, String title, String artist, String album, long durationMs,
                        String artworkUrl, String audioUrl) {
        OffsetDateTime t = OffsetDateTime.of(2026, 3, 3, 0, 0, 0, 0, ZoneOffset.UTC);
        return Track.builder()
                .id(UUID.fromString(id))
                .title(title).artist(artist).album(album)
                .durationMs(durationMs)
                .artworkUrl(artworkUrl)
                .audioUrl(audioUrl)
                .createdAt(t).updatedAt(t)
                .build();
    }

    @Test
    void should_ServeCatalogContract_when_ListAll() throws Exception {
        // Catalog khớp fixture (cùng id/field) — bất kỳ đổi field nào cũng break cả 2 phía.
        List<Track> catalog = List.of(
                track("20000000-0000-4000-8000-000000000001", "Play It Safe", "Julia Wolf",
                        "Girls In Purgatory (Full of Grace)", 159000,
                        "/figma/happy-hits.png",
                        "/api/v1/tracks/20000000-0000-4000-8000-000000000001/audio"),
                track("20000000-0000-4000-8000-000000000003", "Free Spirit", "Khalid",
                        "Free Spirit (Explicit)", 182000, null,
                        "/api/v1/tracks/20000000-0000-4000-8000-000000000003/audio"),
                track("20000000-0000-4000-8000-000000000006", "A Moment Apart", "ODESZA",
                        "A Moment Apart", 234000, null,
                        "/api/v1/tracks/20000000-0000-4000-8000-000000000006/audio"));
        List<TrackResponse> responses = catalog.stream().map(TrackResponse::from).toList();
        when(listTracksUseCase.execute()).thenReturn(responses);

        MvcResult result = mockMvc.perform(get("/api/v1/tracks"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode actual = objectMapper.readTree(result.getResponse().getContentAsString());
        // Envelope {success:true, data:[...]} — chốt cả cấu trúc wrapper lẫn dữ liệu.
        assertThat(actual.path("success").asBoolean()).isTrue();
        assertThat(actual.path("data").isArray()).isTrue();

        // Surefire CWD = module dir (backend/track-service) → ../contracts = backend/contracts.
        JsonNode expected = objectMapper.readTree(Files.readString(CONTRACT));
        // So sánh mảng data; bỏ qua timestamp/message volatile của envelope.
        assertThat(actual.path("data")).isEqualTo(expected.path("data"));
    }
}
