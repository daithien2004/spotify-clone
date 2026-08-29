# Project Status — Spotify Clone

> **Tài liệu sống (single source of truth) cho trạng thái dự án.**
> Cập nhật khi xong milestone: giai đoạn, inventory, next actions. Dữ liệu ngày: **2026-08-29**.
> Trước khi bắt tay bất kỳ task nào, đọc file này (theo `CLAUDE.md`) để nắm tổng thể FE/BE.

## Giai đoạn hiện tại

**Phase E: FE nối API thật (playlist/tracks/player) + backend track MinIO.**

| Phía | Trạng thái |
|---|---|
| **Frontend** | UI theo Figma (Home, Playlist, Header-search, Player, token system, unified scrollbar). Đã **nối API thật** qua Gateway: playlist metadata (`GET /playlists`, `/playlists/{id}`, `/playlists/{id}/tracks`), track metadata batch (`GET /tracks?ids=`), **Player phát audio từ MinIO** (`/tracks/{id}/audio` streaming, byte-range seek). Auth nối thật. `/playlist/*` **đã gate bắt login** (middleware). Chỉ Home feed + search index còn dùng mock. |
| **Backend** | `auth` (JWT + OAuth2 Google qua Gateway, login/logout/register, Kafka basic) + `playlist` (LexoRank: add/get track, reorder, rebalance async; **có metadata Playlist** + GET list/detail + seed) + `track` (catalog metadata CRUD + batch GET, **phase 2: MinIO upload + streaming byte-range**). `user` / `search` service vẫn **Backlog** (xem `domain.md`). |
| **Gateway** | Spring Cloud Gateway — route auth 8081 / playlist 8084 / track 8085 + JWT validation filter (certs). |

## Frontend inventory

### Routes (`frontend/app/`)
- `/` — Home feed (container-query grid)
- `/playlist/[id]` — màn playlist, dữ liệu **thật** (playlist-service metadata + track-service batch GET, join theo lexoRank)
- `/login`, `/register`, `/forgot-password`, `/oauth2/callback`
- `error.tsx` / `loading.tsx` / `not-found.tsx` — App Router chuẩn
- **Gating:** `middleware.ts` — chỉ `/` + trang auth là public; chưa có `auth-token` cookie → redirect `/login` (playlist không còn là demo page, backend yêu cầu JWT)
- **Không còn route `/search`** — SearchBar sống trên header TopNav

### Components
- `MainLayout` — 3 panel + drag-resize sidebar (`--left-w`/`--right-w`, localStorage)
- `TopNav` — chevron back/forward + `SearchBar` giữa + avatar dropdown (Account/Settings/Logout)
- `LibraryNav` — Home / Your Library + danh sách playlist **từ API** (`GET /playlists`), fallback mock khi API lỗi
- `HomeFeed` + `MusicCard` (cva, memo), `SectionHeader`
- `FriendActivity` — panel phải
- `Player` + `player/` (TrackInfo, PlaybackControls, PlayerProgress, VolumeControl, ControlButton) — **HTML5 `<audio>` phát stream MinIO** (`/tracks/{id}/audio`), byte-range seek, duration thật từ metadata, `onEnded → next`, hidden audio ko render
- `usePlayerStore` — queue + playQueue/next/previous/addToQueue; progress/volume; persist volume+currentTrack (localStorage)
- `playlist/` (TrackTable, TrackRow + context menu)
- `search/` (SearchBar combobox, SearchResultRow, SearchSuggestionRow)

### Design system
- `tokens.css` + `@theme inline` trong `globals.css` → utility `bg-*/text-*/border-*` (token-only, cấm arbitrary-hex)
- **Unified scrollbar** (global: webkit + Firefox) + `@utility no-scrollbar`
- State: Zustand (`usePlayerStore`, `useAuthStore`) — **granular selectors**; React Query (`queryKeys.ts`)

### Services & tests
- `services/api/playlistService.ts` — `GET /playlists`, `/playlists/{id}`, `/playlists/{id}/tracks` (unwrap envelope → domain types)
- `services/api/trackService.ts` — `GET /tracks?ids=` batch metadata (stream audio dùng trực tiếp làm `<audio src>`)
- `lib/adapters.ts` — map DTO backend → `TrackItem`/`Playlist`/store `Track` (durationMs→s, cover fallback, formatDuration)
- `lib/api-client.ts` — axios instance: baseURL Gateway (`/api/v1`), envelope `ApiResponse<T>` + `unwrap()`, `resolveApiUrl()`, **refresh-token flow** (401 → `/auth/refresh`, queue các request chờ, clearAuth logout)
- `services/search/searchService.ts` — logic search thuần (mock `TRACK_INDEX`), đã TDD
- `services/api/authService.ts` — auth qua Gateway
- Vitest: **26 test xanh** (searchService 11, SearchBar 8, PlayerProgress 3, usePlayerStore 4)

## Backend inventory

### `backend/` — **Maven multi-module** (chuyển từ monolith → microservices, 2026-08-29)
- `common-lib` — `ApiResponse`, `GlobalResponseWrapper`, `GatewayHeaderFilter`, `ServiceSecurityConfig` (jar chia sẻ)
- `auth-service` (port **8081**) — Clean Arch: domain / application (usecase, port) / infrastructure (persistence, security **oauth2**, **TOTP 2FA**, messaging **Kafka**, Redis session) / presentation (controller)
- `track-service` (port **8085**) — Clean Arch: domain (`Track`, `TrackAudioFile`, `TrackAudioRange`, events, `TrackRepository`/`TrackAudioRepository` port) / application (Create/GetByIds-batch/Update/**UploadAudio/GetAudio** usecase) / infrastructure (JPA adapter/mapper, **MinIO client**, log-only event publisher, exception handler) / presentation (controller)
  - Metadata endpoints: `POST`/`PUT` `/api/v1/tracks`, `GET /api/v1/tracks/{id}`, `GET /api/v1/tracks?ids=a,b,c` (giữ thứ tự input) — DB `track_db`:5434, schema via Flyway `V1__init_track_schema` + `V2__seed_tracks.sql` (6 track cố định, `audio_url` → streaming endpoint)
  - **Phase 2 MinIO (2026-08-29):** `TrackAudioController` — `PUT /api/v1/tracks/{trackId}/audio` (upload MultipartFile → MinIO bucket `tracks`) + `GET .../audio` (**streaming HTTP byte-range** `Accept-Ranges/Content-Range`, seek được trong browser). Seed volatile vào MinIO lúc boot bởi `TrackAudioSeedInitializer`. Cấu hình `MinioConfig` (env `MINIO_*`, endpoint `:9010`)
  - **Test: 19 xanh** (Create 5, GetByIds 3, Update 3, UploadAudio 4, GetAudio 2, Mapper 2). `Track.Uploaded`/`TrackAudioUploaded` event log-only (search/notify còn Backlog)
- `playlist-service` (port **8084**) — Clean Arch: domain (entity, **LexoRankService** thuần, event) / application (usecase) / infrastructure (persistence adapter/mapper) / presentation (controller)
  - Endpoints: `GET /api/v1/playlists` (list summary), `GET /api/v1/playlists/{playlistId}` (**metadata** — Playlist), `POST`/`GET` `/api/v1/playlists/{playlistId}/tracks`, `PUT /api/v1/playlists/{playlistId}/tracks/{playlistTrackId}/reorder`
  - **Metadata (2026-08-29):** `Playlist` entity + `PlaylistRepository` + `GetPlaylistById`/`ListPlaylists` usecase — DB `V2__add_playlists.sql` (bảng `playlists`) + `V3__seed_playlists.sql` (3 playlist cố định, fixed UUID join với track-service `V2__seed_tracks.sql`)
  - **LexoRank:** full charset `0-9A-Z_a-z` (63 ký tự) — midpoint theo vị trí charset; rebalance trigger khi append; `PlaylistRebalanceScheduler` 5 phút + `AsyncConfig` + `@EnableScheduling`
  - Error contract (convention §2): `GlobalExceptionHandler` (`infrastructure/exception/`) → 400/500, body `ApiResponse.error(...)`; `@Valid` + `@NotNull`
  - **Test: 27 xanh** (LexoRank 8, Scheduler 2, AddTrack 5, Reorder 5, GetPlaylistById 2, GetTracks 2, ListPlaylists 2, Rebalance 1)
- **Database-per-service:** `auth_db` (5432) + `playlist_db` (5433) + **`track_db` (5434)** trong `docker-compose.yml` (migrations consolidated 2026-08-29: mỗi service đánh số riêng — auth gộp V1-V4,V6 + bỏ bảng dead `security_tokens`; playlist đổi tên V5 → V1, thêm V2 playlists + V3 seed; track V1 schema + V2 seed)
- Infra môi trường: `docker-compose.yml` (**MinIO** `:9010`/console `:9011` bucket `tracks` + 3×Postgres `auth:5432`/`playlist:5433`/`track:5434` + Redis/Kafka/kafka-ui), `.env`
- `common` cũ: `GlobalExceptionHandler` + auth exception → về auth-service (common cũ từng import ngược auth — đã gỡ phụ thuộc). **`GlobalResponseWrapperTest` (5 test)** cho envelope

### `gateway/` — Spring Cloud Gateway
- Route config: `auth-oauth2`, auth login/register/refresh (bypass filter), protected `auth/**`, `playlists/**` → 8084, `tracks/**` → 8085 + JWT validation filter (certs)

### Skills / workflow
- **`be-workflow`** (`.claude/skills/be-workflow/`) — pipeline bắt buộc cho task BE: 4 bước + 2 gate, verify `./mvnw test` + Clean Arch self-check + review độc lập.
- **`fe-workflow`** — pipeline FE tương ứng.
- **Plugin cộng đồng đã cài: `engineering-advanced-skills@claude-code-skills`** (2.9.0) — `migration-architect`, `database-designer`, `api-design-reviewer`, `api-test-suite-builder`, `pr-review-expert`, `ship-gate`… dùng qua be-workflow bước 3/4.
- **Đã bỏ 3 skill tự viết cũ** (`db-migration`/`kafka-event`/`security-review`) — không có SKILL.md; thay bằng skill cộng đồng. Kafka giữ inline theo `domain.md`.

### Backlog (theo `domain.md`)
- `user-service` (profile, follows), `search-service` (Elasticsearch). Track upload/stream/MinIO **đã xong backend (phase 2)** — còn thiếu CDN/auth-cache + E2E upload thật qua UI

## Next actions (dự kiến — để chủ dự án duyệt)
1. ✅ Tạo `be-workflow` + cài plugin `engineering-advanced-skills` + bỏ skill cũ (2026-08-29)
2. ✅ Tách backend monolith → microservices (auth 8081, playlist 8084, common-lib, database-per-service) (2026-08-29) — build xanh 17 test, gateway retarget xong
3. ✅ Chạy stack thật (docker-compose) + 2 service chạy 8081/8084, DB đã verify bảng qua Flyway (2026-08-29)
4. ✅ Sửa nợ kỹ thuật playlist (2026-08-29): Rebalance dùng domain port thay vì JPA trong application; publisher PascalCase; `@EnableAsync`; **thêm add/get track**; **LexoRank full charset + rebalance on append + scheduler định kỳ** — build xanh **23 test playlist**
5. ✅ Thêm `GlobalExceptionHandler` + `ApiResponse<T>` envelope + `@Valid` cho playlist endpoints (2026-08-29) — verify runtime 5/5 case
6. ✅ Tạo `track-service` (2026-08-29): catalog metadata CRUD + batch GET, DB `track_db`:5434, gateway route, `Track.Uploaded` (log-only), 13 test xanh — unblock FE join trackId→metadata
7. ✅ **Phase E: FE nối API thật + Player MinIO + gating /playlist** (2026-08-29):
   - Playlist page đọc **metadata thật** từ playlist-service (`GET /playlists/{id}` + `GET .../tracks`) và track-service batch (`GET /tracks?ids=`), join theo lexoRank; LibraryNav list playlist từ API (fallback mock khi lỗi)
   - **Player phát audio từ MinIO** — HTML5 `<audio src="/api/v1/tracks/{id}/audio">` (streaming byte-range qua gateway), duration thật từ metadata, seek/volume/queue đầy đủ (`usePlayerStore`)
   - **Gating `/playlist`** — `middleware.ts` redirect `/login` khi chưa có `auth-token` (chỉ `/` + trang auth public); api-client có **refresh-token flow** (401 → `/auth/refresh`)
   - Tests: FE 26 xanh, backend **64 xanh** (common-lib 5, auth 13, playlist 27, track 19)
8. **Smoke test E2E qua cổng 9000** — docker-compose **mới gồm MinIO + track-db**, verify: login → list playlist → mở `/playlist/{id}` → **Player phát audio MinIO thật + seek** (luồng chưa chạy E2E thủ công trong phiên)
9. (Sau) search-service Elasticsearch; user-service; track upload thật qua UI/FE + CDN/auth-cache cho streaming

## Cách dùng
- **AI:** đọc file này khi bắt đầu mọi task/session; khi kết thúc milestone → **cập nhật** giai đoạn + inventory + next actions.
- **Con người:** duyệt "Next actions", sửa trực tiếp file khi rẽ hướng.