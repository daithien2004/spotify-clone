package com.spotify.search.infrastructure.integration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.infrastructure.search.TrackElasticsearchRepository;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho tìm kiếm THẬT trên Elasticsearch container — xác minh mapping
 * index `tracks`, tokenization/full-text + fuzziness của multi_match, và index/remove
 * round-trip. Bỏ qua Spring context (Kafka/security) — chỉ test adapter ES cô lập.
 *
 * <p>Đây là tầng integration mà unit test (mock) không phủ: bug mapping index hoặc
 * query ES trả sai sẽ không bắt được nếu chỉ mock ElasticsearchClient.
 */
@Testcontainers
class TrackElasticsearchRepositoryIntegrationTest {

    // Image 8.15.3 đã có sẵn local (dùng cho dev search-service) — tránh pull 7.17 mất phút.
    // ES 8.x bật security/HTTPS mặc định — tắt để client REST (HTTP) kết nối được như dev.
    @Container
    static final ElasticsearchContainer ES =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.3")
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
                    .withEnv("http.host", "0.0.0.0")
                    .withEnv("transport.type", "netty4")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");

    private static ElasticsearchClient client;
    private static TrackElasticsearchRepository repository;

    @BeforeAll
    static void setUp() throws Exception {
        // Build client giống bean Spring (data-elasticsearch auto-config) nhưng trỏ container.
        RestClient restClient = RestClient.builder(HttpHost.create(ES.getHttpHostAddress())).build();
        client = new ElasticsearchClient(
                new co.elastic.clients.transport.rest_client.RestClientTransport(restClient, new JacksonJsonpMapper()));
        repository = new TrackElasticsearchRepository(client);
        awaitClusterReady();
        repository.ensureIndex();
    }

    /** Chờ cluster ES sẵn sàng (health yellow/green) trước khi index — tránh connection bị đóng giữa chừng nếu node chưa ổn định. */
    private static void awaitClusterReady() throws Exception {
        for (int i = 0; i < 60; i++) {
            try {
                HealthResponse health = client.cluster().health(h -> h);
                String status = health.status().jsonValue();
                if ("yellow".equals(status) || "green".equals(status)) {
                    return;
                }
            } catch (Exception ignored) {
                // node chưa lên — retry
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Elasticsearch container chưa sẵn sàng sau 60s");
    }

    /** Ép refresh index thật (thay vì sleep) — đảm bảo document searchable ngay. */
    private void refresh() throws Exception {
        client.indices().refresh(r -> r.index("tracks"));
    }

    private TrackSearchDocument doc(UUID id, String title, String artist, String album) {
        return new TrackSearchDocument(id, title, artist, album, 200_000L, "/art.png", "/audio");
    }

    @Test
    @DisplayName("search theo title tìm đúng track đã index (full-text thật)")
    void should_FindByTitle_when_Indexed() throws Exception {
        TrackSearchDocument freeSpirit = doc(UUID.randomUUID(), "Free Spirit", "Khalid", "Free Spirit");
        repository.index(freeSpirit);
        refresh();

        List<TrackSearchDocument> results = repository.search("spirit", 10);

        assertThat(results).extracting(TrackSearchDocument::title)
                .contains(freeSpirit.title());
    }

    @Test
    @DisplayName("search theo artist tìm đúng track (field artist^2)")
    void should_FindByArtist_when_Indexed() throws Exception {
        TrackSearchDocument track = doc(UUID.randomUUID(), "Some Song", "ODESZA", "Album");
        repository.index(track);
        refresh();

        List<TrackSearchDocument> results = repository.search("odesza", 10);

        assertThat(results).extracting(TrackSearchDocument::title).contains(track.title());
    }

    @Test
    @DisplayName("remove track biến mất khỏi kết quả tìm kiếm")
    void should_NotFindTrack_when_Removed() throws Exception {
        TrackSearchDocument track = doc(UUID.randomUUID(), "To Remove", "Artist X", "Album Y");
        repository.index(track);
        refresh();

        repository.remove(track.id());
        refresh();

        List<TrackSearchDocument> results = repository.search("Remove", 10);
        assertThat(results).extracting(TrackSearchDocument::id).doesNotContain(track.id());
    }

    @Test
    @DisplayName("search không khớp trả danh sách rỗng")
    void should_ReturnEmpty_when_NoMatch() throws Exception {
        List<TrackSearchDocument> results = repository.search("zzzz-nonexistent-keyword", 10);
        assertThat(results).isEmpty();
    }
}
