# Design: Search-service (Elasticsearch)

**Ngày:** 2026-08-29
**Sub-project:** #1 của MVP Core
**Trạng thái:** Được duyệt (scope: MVP core, search = Elasticsearch, index tracks)

## 1. Mục tiêu

Cho phép FE tìm kiếm **bài hát** theo `title` / `artist` / `album`. Hiện tại FE search trên
`TRACK_INDEX` mock (trong `frontend/lib/musicData.ts` — dẫn xuất từ PLAYLISTS mock). Thay bằng
API thật từ service mới **`search-service`** (port **8086**) lưu index trong **Elasticsearch**.

Out of scope (ghi lại để không bị kéo dài): search playlist/artist, filter genre, highlight,
pagination sâu, DLQ, synonyms — MVP cắt hết.

## 2. Kiến trúc tổng thể

```
track-service ──(Kafka: spotify.track.events)──▶ search-service ──▶ Elasticsearch :9200
   │  GET /api/v1/tracks (list-all, cho bootstrap)
   │
gateway /api/v1/search/** ──▶ 8086  ──▶ GET /search/tracks?q=
```

- **Event-driven:** track-service publish domain events ra Kafka. search-service consume → index/remove.
- **Bootstrap:** data lịch sử (seed 6 tracks + bất kỳ track nào index trước khi ES bật) được pull
  một lần lúc startup qua `GET /tracks` (list-all). Giải quyết ca: ES được bật sau khi track đã tồn tại.
- **Database-per-service:** search-service KHÔNG đọc thẳng `track_db` — chỉ nhận qua event + bootstrap API.

## 3. Components (Clean Arch theo pattern track-service)

### `search-service/src/main/java/com/spotify/search/`

| Layer | File | Nội dung |
|---|---|---|
| `domain/entity/` | `TrackSearchDocument.java` | record/POJO thuần: `id, title, artist, album, artworkUrl, audioUrl, durationMs` |
| `domain/repository/` | `TrackSearchRepository.java` | port: `index(TrackSearchDocument)`, `remove(UUID id)`, `List<TrackSearchDocument> search(String q, int limit)` |
| `application/usecase/` | `IndexTrackUseCase.java` + Impl | nhận `IndexTrackCommand` (từ event hoặc bootstrap) → gọi repo.index |
| | `RemoveTrackUseCase.java` + Impl | `RemoveTrackCommand(trackId)` → repo.remove |
| | `SearchTracksUseCase.java` + Impl | `SearchTracksCommand(q, limit)` → repo.search → DTO |
| `application/dto/` | `SearchTrackItem.java` | record: `id, title, artist, album, artworkUrl, audioUrl, durationMs` |
| | `IndexTrackCommand.java`, `RemoveTrackCommand.java`, `SearchTracksCommand.java` | records |
| `infrastructure/search/` | `TrackElasticsearchRepository.java` | adapter Elasticsearch client, index name `tracks` |
| `infrastructure/kafka/` | `TrackEventConsumer.java` | `@KafkaListener(topic spotify.track.events)`, deserialize `TrackEventEnvelope`, route tới Index/Remove usecase |
| `infrastructure/bootstrap/` | `TrackIndexBootstrap.java` | `ApplicationRunner`: `GET {track-service}/api/v1/tracks` → bulk index (chỉ nếu index đang trống/hoặc reindex toàn bộ) |
| `infrastructure/config/` | `ElasticsearchConfig.java` | tạo `RestClient`/client từ `ELASTICSEARCH_URIS` (default `http://localhost:9200`); `TrackEventEnvelope` dùng Jackson |
| `infrastructure/seed/` | (tùy chọn) — KHÔNG cần seed riêng, dữ liệu qua bootstrap |
| `infrastructure/exception/` | `GlobalExceptionHandler.java` (để đồng bộ error contract 400/500 + `ApiResponse.error`) |
| `presentation/controller/` | `SearchController.java` | `GET /api/v1/search/tracks?q=&limit=` |
| `TrackSearchServiceApplication.java` | main + `@EnableKafka` |

### Kafka event contract (dùng chung giữa track-service/search-service)

Topic: **`spotify.track.events`**

```json
// TrackEventEnvelope — search-service nhận record value
{
  "eventType": "TRACK_UPLOADED" | "TRACK_UPDATED" | "TRACK_REMOVED" | "TRACK_AUDIO_UPLOADED",
  "eventId": "uuid",
  "occurredOn": "ISO-8601",
  "track": {
    "id": "uuid", "title": "…", "artist": "…", "album": "…",
    "durationMs": 159000, "artworkUrl": "…", "audioUrl": "…"
  }
}
```

- `TRACK_UPLOADED` / `TRACK_UPDATED` → IndexTrackUseCase
- `TRACK_REMOVED` → RemoveTrackUseCase
- `TRACK_AUDIO_UPLOADED` → không ảnh hưởng index (không cần xử lý; giữ topic chung cho đơn giản)

### Changes bắt buộc ở service cũ

**track-service (8085):**
1. **`TrackUploaded` event** — mở rộng payload: thay vì `(trackId, title, artist)` → mang **toàn bộ `Track`**
   (id, title, artist, album, durationMs, artworkUrl, audioUrl). Cập nhật `CreateTrackUseCaseImpl`.
   Thêm **`TrackUpdated`** event (publish trong `UpdateTrackUseCaseImpl`) và **`TrackRemoved`**
   (cho delete — xem sub-project #5; có thể thêm sau, không bắt buộc nhóm này).
2. **Publisher Kafka thật** — thay thế `TrackLogDomainEventPublisher` bằng `TrackKafkaDomainEventPublisher`
   (giữ log + gửi Kafka). Thêm dependency `spring-kafka`, cấu hình `spring.kafka.*` như auth-service
   (bootstrap-servers `localhost:9092`, producer JsonSerializer, flag `spring.kafka.enabled`).
3. **`GET /api/v1/tracks` (list-all)** — mở rộng `TrackController` để khi `ids` không có → trả toàn bộ
   (hoặc thêm param `list`). Dùng cho bootstrap.

**docker-compose.yml:**
- Thêm service `elasticsearch` (image `docker.elastic.co/elasticsearch/elasticsearch:8.x`, single-node
  `discovery.type=single-node`, tắt security `xpack.security.enabled=false`, port **9200**, memory limit
  ~1GB `ES_JAVA_OPTS=-Xms512m -Xmx512m`, volume `es_data`).

**gateway:**
- Route `/api/v1/search/**` → `http://localhost:8086`, qua JWT filter (như track/playlist).

## 4. API contract

```
GET /api/v1/search/tracks?q=julia&limit=10
Authorization: Bearer <JWT> (qua gateway, inject X-User-Id)
→ 200 ApiResponse<List<SearchTrackItem>>
→ 400 nếu q rỗng/thiếu
→ 500 lỗi Elasticsearch (GlobalExceptionHandler)
```

`SearchTrackItem` fields: `id, title, artist, album, artworkUrl, audioUrl, durationMs`.

## 5. Elasticsearch index mapping

Index `tracks`:

```json
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
```

Query cho `q`: multi-match trên `title^3, artist^2, album` — ưu tiên tên bài hát, rồi artist,
chứa `fuzziness: AUTO` nhẹ cho typo. Limit default 10, cap 50.

## 6. Data flow

1. **Startup:** `TrackIndexBootstrap` gọi `GET http://localhost:8085/api/v1/tracks` → bulk index toàn bộ vào
   ES (idempotent bằng `id`). Log số lượng.
2. **Runtime create/update track:** `CreateTrackUseCaseImpl`/`UpdateTrackUseCaseImpl` publish event →
   `TrackKafkaDomainEventPublisher` gửi Kafka → `TrackEventConsumer` → `IndexTrackUseCase` → `Index(request)`.
3. **FE search:** SearchBar (debounce) → `GET /api/v1/search/tracks?q=` → render SearchResult.
4. **Failure:** nếu ES không khả dụng lúc bootstrap → log lỗi, **KHÔNG crash** app (bootstrap bắt exception,
   shutdown graceful). Consumer lỗi → log + (không DLQ, ghi chú).

## 7. FE changes

- `frontend/services/api/searchService.ts` (mới): `GET /api/v1/search/tracks` qua `api-client` (unwrap envelope).
  Trả `SearchTrackItem[]` → map sang domain.
- `SearchBar` (hoặc top-nav hiện tại): bỏ `TRACK_INDEX` mock, gọi `searchService.search(q)` (debounce ~300ms, cancel cũ).
- Bỏ import `TRACK_INDEX` khỏi `services/search/searchService.ts` (hoặc giữ pure-logic cũ nhưng không dùng).
  Quyết định: giữ `searchService.ts` là class thuần với 2 hàm (`searchTracks`, `searchSuggestions`) nhưng
  **thêm adapter** `remoteSearch` bên cạnh, để không phá test cũ — chi tiết ở plan.

## 8. Testing (TDD — mocks, không cần ES container cho unit test)

| Module | Test | ~số case |
|---|---|---|
| search-service | `SearchTracksUseCaseImplTest` | 4–5 (match title/artist/album, limit, empty q, fuzziness) |
| | `IndexTrackUseCaseImplTest` | 3 (index mới, index lại, validate payload) |
| | `RemoveTrackUseCaseImplTest` | 2 |
| | `TrackElasticsearchRepository` — KHÔNG test unit (integration sau, cần ES) | — |
| track-service | `CreateTrackUseCaseImplTest` (mở rộng: publish event đủ payload) | thêm 2 |
| | `TrackKafkaDomainEventPublisherTest` | 2 (enabled=true gửi, false skips) |

Cộng thêm: build xanh `./mvnw test` (5 module cũ + search-service mới). FE: viết `searchService` test thật
(mock axios) 2–3 case.

## 9. Things to verify at runtime (smoke)

Sau khi code xong, chạy stack + smoke tay:
1. `docker-compose up -d` (có ES) → track-service + search-service + gateway chạy 8085/8086/9000.
2. `curl 'http://localhost:9000/api/v1/search/tracks?q=Khalid'` với JWT → trả docs từ ES.
3. Tạo track mới qua `POST /api/v1/tracks` → event → ES → search thấy track mới (event-driven).
4. FE login → gõ SearchBar "julia" → ra Julia Wolf.

## 10. Deliverables (files)

Search-service mới (Clean Arch, theo cấu trúc track-service) + các thay đổi đã liệt kê ở mục 3
(track-service event/publisher, controller list-all, docker-compose ES, gateway route, FE search).

## 11. Acceptance criteria

- [ ] `GET /api/v1/search/tracks?q=` trả danh sách đúng title/artist/album match, thứ tự ưu tiên title→artist→album
- [ ] Event UPLOADED/UPDATED từ track-service → index vào ES trong < 2s (log xác nhận)
- [ ] Bootstrap reindex đầy đủ khi ES mới (không mất track có sẵn)
- [ ] FE SearchBar dùng API thật (không dùng TRACK_INDEX)
- [ ] `./mvnw test` xanh toàn bộ (5 module cũ + search-service), FE vitest xanh
- [ ] Gateway route `/api/v1/search/**` bảo vệ JWT (không token → 401)
- [ ] Search out-of-scope items được ghi chú rõ (playlist search, pagination sâu, DLQ)
</tool_calls>