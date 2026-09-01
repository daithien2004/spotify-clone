package com.spotify.search.infrastructure.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.common.event.TrackEventEnvelope.TrackPayload;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3 — Contract test phía CONSUMER (search-service).
 *
 * <p>Xác minh {@link RestTrackBootstrapFetcher} parse được đúng JSON mà provider
 * (track-service {@code GET /api/v1/tracks} list-all) trả về, theo fixture chung
 * {repo}/backend/contracts/track-list-all.contract.json.
 *
 * <p>Contract đang có drift âm thầm: consumer deserialize từng phần tử thành
 * {@link TrackPayload} (7 field, id String) trong khi provider trả {@code TrackResponse}
 * (9 field, id UUID, thêm createdAt/updatedAt) — chạy được chỉ nhờ Jackson lenient bỏ qua
 * field thừa. Nếu provider đổi/thiếu field mà consumer cần, bootstrap reindex sẽ âm thầm
 * index dữ liệu rỗng. Test này đảm bảo 7 field consumer cần vẫn parse đúng từ contract thật.
 */
class TrackListAllContractConsumerTest {

    private static final Path CONTRACT =
            Path.of("..", "contracts", "track-list-all.contract.json");

    private static HttpServer server;
    private static RestTrackBootstrapFetcher fetcher;

    @BeforeAll
    static void setUp() throws Exception {
        byte[] body = Files.readAllBytes(Path.of("").toAbsolutePath().resolve(CONTRACT));

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/tracks", exchange -> {
            byte[] resp = body;
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();

        // Surefire CWD = module dir (backend/search-service) → ../contracts = backend/contracts.
        fetcher = new RestTrackBootstrapFetcher("http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void should_ParseCatalogContract_when_Fetched() {
        List<TrackPayload> tracks = fetcher.fetchAll();

        assertThat(tracks).hasSize(3);

        // 7 field contract mà consumer phụ thuộc phải parse nguyên vẹn từ provider.
        TrackPayload first = tracks.get(0);
        assertThat(first.id()).isEqualTo("20000000-0000-4000-8000-000000000001");
        assertThat(first.title()).isEqualTo("Play It Safe");
        assertThat(first.artist()).isEqualTo("Julia Wolf");
        assertThat(first.album()).isEqualTo("Girls In Purgatory (Full of Grace)");
        assertThat(first.durationMs()).isEqualTo(159_000L);
        assertThat(first.artworkUrl()).isEqualTo("/figma/happy-hits.png");
        assertThat(first.audioUrl())
                .isEqualTo("/api/v1/tracks/20000000-0000-4000-8000-000000000001/audio");

        // Track không artworkUrl (null bị provider bỏ key) → parse thành null, không crash.
        TrackPayload odesza = tracks.get(2);
        assertThat(odesza.title()).isEqualTo("A Moment Apart");
        assertThat(odesza.artworkUrl()).isNull();
    }

    @Test
    void fixtureMustBeValidJson() throws Exception {
        // Chốt fixture là JSON hợp lệ (đọc được bằng ObjectMapper) — chống fixture hỏng âm thầm.
        new ObjectMapper().readTree(Files.readAllBytes(CONTRACT));
        assertThat(new String(Files.readAllBytes(CONTRACT), StandardCharsets.UTF_8))
                .contains("\"Play It Safe\"");
    }
}
