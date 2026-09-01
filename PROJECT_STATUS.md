# Project Status — Spotify Clone

> **Tài liệu sống (single source of truth) cho trạng thái dự án.**
> Cập nhật khi xong milestone: giai đoạn, inventory, next actions. Dữ liệu ngày: **2026-08-31**.
> Trước khi bắt tay bất kỳ task nào, đọc file này (theo `CLAUDE.md`) để nắm tổng thể FE/BE.

## Giai đoạn hiện tại

**Phase E: FE nối API thật (playlist/tracks/player) + backend track MinIO.**
**Phase E+ (2026-08-30): search-service (Elasticsearch) — endpoint search + FE SearchBar nối API thật.**
**Auth Completion (2026-08-30→31): hoàn thiện auth FE+BE — reset password, email verification, profile/account, TOTP 2FA.**
**Smoke Testing Tooling (2026-08-31→09-01): TOTP helper (`scripts/totp.mjs`) + `scripts/smoke-auth.sh` + Mailpit + Playwright E2E. Runtime full-stack **đã chạy & xanh**: `smoke-auth.sh run` **PASS=35 FAIL=0** + full Playwright E2E **4/4 pass** (gating 2 + auth 2) trên chromium thật.**

| Phía | Trạng thái |
|---|---|
| **Frontend** | UI theo Figma (Home, Playlist, Header-search, Player, token system, unified scrollbar). Đã **nối API thật** qua Gateway: playlist metadata (`GET /playlists`, `/playlists/{id}`, `/playlists/{id}/tracks`), track metadata batch (`GET /tracks?ids=`), **Player phát audio từ MinIO** (`/tracks/{id}/audio` streaming, byte-range seek), **SearchBar query `GET /search/tracks` thật** (debounce 300ms → React Query, suggestion từ kết quả live). **Auth hoàn thiện (Auth Completion):** login **bước 2 TOTP** (mfaToken memory-only, ADR D3), forgot/reset-password page thật, verify-email page, **/account** (profile + avatar URL text-only + email-verify banner/resend + 2FA enroll QR/verify/disable), BootstrapAuth `/me` revalidate (ADR D7). `/playlist/*` + `/account` **đã gate bắt login** (middleware). |
| **Backend** | `auth` (JWT + OAuth2 Google qua Gateway, login/logout/register, **TOTP 2FA local**, **PATCH /me profile**, **email verification auto-send**, **forgot/reset password**, cookie factory thống nhất ADR D5, Kafka basic) + `playlist` (LexoRank: add/get track, reorder, rebalance async; **có metadata Playlist** + GET list/detail + seed) + `track` (catalog metadata CRUD + batch GET + **list-all `GET /tracks`**, **phase 2: MinIO upload + streaming byte-range**, **Kafka publish Track events**) + **`search` (port 8086, Elasticsearch: index `tracks`, consumer Kafka + bootstrap reindex, `GET /search/tracks`)**. `user`-service vẫn **Backlog** (xem `domain.md`). |
| **Gateway** | Spring Cloud Gateway — route auth 8081 / playlist 8084 / track 8085 / **search 8086** + JWT validation filter (certs) + **permitAll 5 public auth routes** (forgot/reset/send-verification/verify-email/2fa-verify-login). |

## Frontend inventory

### Routes (`frontend/app/`)
- `/` — Home feed (container-query grid)
- `/playlist/[id]` — màn playlist, dữ liệu **thật** (playlist-service metadata + track-service batch GET, join theo lexoRank)
- `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email`, `/oauth2/callback`, **`/account`** (profile + 2FA + email banner)
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
- `services/api/searchService.ts` — **`GET /search/tracks?q=&limit=`** thật (spec §4), unwrap envelope → `SearchItem[]`; blank q short-circuit
- `lib/adapters.ts` — map DTO backend → `TrackItem`/`Playlist`/store `Track` (durationMs→s, cover fallback, formatDuration)
- `lib/api-client.ts` — axios instance: baseURL Gateway (`/api/v1`), envelope `ApiResponse<T>` + `unwrap()`, `resolveApiUrl()`, **refresh-token flow** (401 → `/auth/refresh`, queue các request chờ, clearAuth logout)
- `services/search/searchService.ts` — logic search thuần (giữ nguyên — pure); **SearchBar không còn dùng `TRACK_INDEX`** (spec §7)
- `services/api/authService.ts` — auth qua Gateway (login/register/me/refresh/logout + reset/verify/profile/2FA methods, envelope không unwrap)
- `hooks/useAuth.ts` + `useAuthStore.ts` — granular selectors; hooks: useLogin (mfaRequired early-return ADR D3), useRegister (toast verify-email D6), reset/verify/profile/2FA hooks + `useBootstrapAuth` (revalidate `/me`)
- `lib/validation/auth.ts` — validateEmail/Password/ConfirmPassword/DisplayName/TotpCode (pure)
- Vitest: **53 test xanh (9 test files)** (search-logic, SearchBar, services/api searchService + authService, PlayerProgress, usePlayerStore, useAuth, validation/auth)
- **Playwright E2E (2026-08-31→09-01):** `frontend/e2e/` — `gating.spec.ts` (2 test: `/account`, `/playlist/[id]` bị redirect `/login` khi chưa login — chỉ cần FE dev server) + `auth.spec.ts` (full-stack: register API→verify email từ Mailpit→UI login→`/account` displayName; + 2FA login flow; chạy khi `E2E_FULL=1`, tự skip nếu thiếu env). `playwright.config.ts` (webServer `npm run dev` tự khởi động), chromium đã cài, `npm run test:e2e`. **Full suite 4/4 xanh trên chromium thật** (nhiều lần chạy). Bug phát hiện & fix khi chạy full-stack: FE envelope không `unwrap` (login/register/`/me` persist user `{}` → account page rỗng) fix `authService`; BE `/me` double-wrap (`data.data`) fix `AuthController` trả `result.data()`; rate-limit Bucket4j cứng 10/phút/IP đụng full-suite → configurable qua env (test 100); FE account page sync async user bằng `ProfileForm` key theo `user.id` (tránh setState-in-effect lint).

## Backend inventory

### `backend/` — **Maven multi-module** (chuyển từ monolith → microservices, 2026-08-29)
- `common-lib` — `ApiResponse`, `GlobalResponseWrapper`, `GatewayHeaderFilter`, `ServiceSecurityConfig` (jar chia sẻ)
- `auth-service` (port **8081**) — Clean Arch: domain / application (usecase, port) / infrastructure (persistence, security **oauth2**, **TOTP 2FA**, messaging **Kafka**, Redis session) / presentation (controller)
  - **Auth Completion (2026-08-30→31):** domain `User` TOTP secret + `storePendingTotpSecret/enable/disable`; application: Forgot/Reset password usecase, VerifyTwoFactorSetup (enroll QR, ADR D2: save-secret-then-enable), VerifyTwoFactorLogin (2-step login, single-use mfaToken Redis, ADR D3), UpdateProfile + GetCurrentUser enrich (`emailVerified`/`twoFactorEnabled`), RegisterUseCase **auto-send verification email** (D6); presentation: `POST /auth/forgot-password`, `/reset-password`, `/send-verification`, `/verify-email`, `PATCH /me`, `/2fa/enroll|verify|disable`, `/2fa/verify-login`; **`AuthCookieFactory`** thống nhất path/domain 2 cookie (ADR D5); 2FA brute-force guard (Redis counter) + audit SecurityAuditPublisher; **Test: 39 xanh** (User 4, Email 3, Password 5, Login 4, Register 3, Enroll 3, Verify2faSetup 3, Verify2faLogin 3, Disable 2, UpdateProfile 4, GetCurrentUser 1, TotpAdapter 4)
- `track-service` (port **8085**) — Clean Arch: domain (`Track`, `TrackAudioFile`, `TrackAudioRange`, events, `TrackRepository`/`TrackAudioRepository` port) / application (Create/GetByIds-batch/Update/List-all/**UploadAudio/GetAudio** usecase) / infrastructure (JPA adapter/mapper, **MinIO client**, **Kafka publisher**, exception handler) / presentation (controller)
  - Metadata endpoints: `POST`/`PUT` `/api/v1/tracks`, `GET /api/v1/tracks/{id}`, `GET /api/v1/tracks?ids=a,b,c` (giữ thứ tự input) + **`GET /api/v1/tracks` list-all** (cho search-service bootstrap reindex; permitAll riêng `@Order(1)`) — DB `track_db`:5434, schema via Flyway `V1__init_track_schema` + `V2__seed_tracks.sql` (6 track cố định, `audio_url` → streaming endpoint)
  - **Phase 2 MinIO (2026-08-29):** `TrackAudioController` — `PUT /api/v1/tracks/{trackId}/audio` (upload MultipartFile → MinIO bucket `tracks`) + `GET .../audio` (**streaming HTTP byte-range** `Accept-Ranges/Content-Range`, seek được trong browser). Seed volatile vào MinIO lúc boot bởi `TrackAudioSeedInitializer`. Cấu hình `MinioConfig` (env `MINIO_*`, endpoint `:9010`)
  - **Kafka (2026-08-30):** `TrackKafkaDomainEventPublisher` publish `spotify.track.events` với **full track payload** (Uploaded/Updated/Removed/AudioUploaded), cờ `spring.kafka.enabled` (`KAFKA_ENABLED:true`); bật → bootstrap/consumer search-service nhận. `TrackEventEnvelope`/`TrackPayload` chung ở common-lib
  - **Test: 26 xanh** (Create 5, GetByIds 3, Update 3, List 1, UploadAudio 4, GetAudio 2, Mapper 2, events 6)
- `search-service` (port **8086**) — Clean Arch: domain (`TrackSearchDocument`, `TrackSearchRepository` port) / application (Index/Remove/Search track usecase) / infrastructure (**Elasticsearch adapter** `TrackElasticsearchRepository`, **Kafka consumer** `TrackEventConsumer` group `search-service-group`, **bootstrap** `TrackIndexBootstrap` reindex lúc boot, `RestTrackBootstrapFetcher` GET track-service list-all) / presentation (`SearchController`)
  - Elasticsearch 8.15.3 (docker-compose `:9200`), index `tracks` mapping: title/artist/album text, durationMs long, artwork/audio URL keyword `index:false`. Query multi_match (title^3, artist^2, album) + fuzziness AUTO
  - Endpoints: `GET /api/v1/search/tracks?q=&limit=` (default 10, clamp 1–50, `@Validated`); response wrap `ApiResponse` bởi common-lib `GlobalResponseWrapper`
  - Event flow: track-service Kafka `spotify.track.events` → consumer index/remove; bootstrap đảm bảo history khi ES mới lên (spec §6)
  - **Test: 16 xanh** (Index 4, Remove 2, Search 4, Consumer 4, Bootstrap 2)
  - Endpoints: `GET /api/v1/playlists` (list summary), `GET /api/v1/playlists/{playlistId}` (**metadata** — Playlist), `POST`/`GET` `/api/v1/playlists/{playlistId}/tracks`, `PUT /api/v1/playlists/{playlistId}/tracks/{playlistTrackId}/reorder`
  - **Metadata (2026-08-29):** `Playlist` entity + `PlaylistRepository` + `GetPlaylistById`/`ListPlaylists` usecase — DB `V2__add_playlists.sql` (bảng `playlists`) + `V3__seed_playlists.sql` (3 playlist cố định, fixed UUID join với track-service `V2__seed_tracks.sql`)
  - **LexoRank:** full charset `0-9A-Z_a-z` (63 ký tự) — midpoint theo vị trí charset; rebalance trigger khi append; `PlaylistRebalanceScheduler` 5 phút + `AsyncConfig` + `@EnableScheduling`
  - Error contract (convention §2): `GlobalExceptionHandler` (`infrastructure/exception/`) → 400/500, body `ApiResponse.error(...)`; `@Valid` + `@NotNull`
  - **Test: 27 xanh** (LexoRank 8, Scheduler 2, AddTrack 5, Reorder 5, GetPlaylistById 2, GetTracks 2, ListPlaylists 2, Rebalance 1)
- **Database-per-service:** `auth_db` (5432) + `playlist_db` (5433) + **`track_db` (5434)** trong `docker-compose.yml` (migrations consolidated 2026-08-29: mỗi service đánh số riêng — auth gộp V1-V4,V6 + bỏ bảng dead `security_tokens`; playlist đổi tên V5 → V1, thêm V2 playlists + V3 seed; track V1 schema + V2 seed)
- Infra môi trường: `docker-compose.yml` (**MinIO** `:9010`/console `:9011` bucket `tracks` + 3×Postgres `auth:5432`/`playlist:5433`/`track:5434` + Redis/Kafka/**kafka-ui `:8087`**/**Elasticsearch `:9200`** + volume `es_data`, 2026-08-30 + **Mailpit `:1025`/`:8025`** mail sink 2026-08-31), `.env`
- `common` cũ: `GlobalExceptionHandler` + auth exception → về auth-service (common cũ từng import ngược auth — đã gỡ phụ thuộc). **`GlobalResponseWrapperTest` (5 test)** cho envelope

### `gateway/` — Spring Cloud Gateway
- Route config: `auth-oauth2`, auth login/register/refresh (bypass filter), protected `auth/**`, `playlists/**` → 8084, `tracks/**` → 8085, `search/**` → 8086 + JWT validation filter (certs)

### Skills / workflow
- **`be-workflow`** (`.claude/skills/be-workflow/`) — pipeline bắt buộc cho task BE: 4 bước + 2 gate, verify `./mvnw test` + Clean Arch self-check + review độc lập.
- **`fe-workflow`** — pipeline FE tương ứng.
- **Plugin cộng đồng đã cài: `engineering-advanced-skills@claude-code-skills`** (2.9.0) — `migration-architect`, `database-designer`, `api-design-reviewer`, `api-test-suite-builder`, `pr-review-expert`, `ship-gate`… dùng qua be-workflow bước 3/4.
- **Đã bỏ 3 skill tự viết cũ** (`db-migration`/`kafka-event`/`security-review`) — không có SKILL.md; thay bằng skill cộng đồng. Kafka giữ inline theo `domain.md`.

### Backlog (theo `domain.md`)
- `user-service` (profile, follows). Track upload/stream/MinIO **đã xong backend (phase 2)** — còn thiếu CDN/auth-cache + E2E upload thật qua UI. **search-service đã xong (see above)**

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
8. ✅ **Smoke test E2E qua cổng 9000 (2026-09-01)** — docker-compose gồm MinIO + track-db + Elasticsearch; verify login → list playlist → `/playlist/{id}` → Player audio MinIO Range 206 (+ Content-Range) + SearchBar result từ API thật. Chạy qua `smoke-auth.sh run` → **PASS=35 FAIL=0** qua gateway 9000 (register→verify-email Mailpit→2FA→refresh→forgot/reset + dữ liệu thật + audio Range).
9. ✅ **search-service Elasticsearch (2026-08-30)**: ES 8.15.3 compose, search-service 8086 (domain/application/infrastructure/presentation), consumer Kafka `spotify.track.events`, bootstrap reindex `GET /tracks`, `GET /api/v1/search/tracks` endpoint, gateway route, FE SearchBar nối API thật (debounce 300ms). Backend gate: **common-lib 7 + auth 13 + playlist 27 + track 26 + search 16 = 89 xanh**; FE 28 xanh
10. ✅ **Auth Completion (2026-08-30→31)** — reset password, email verification, profile/account, TOTP 2FA (**đã merge `feature/auth-completion` → main**, 20 commits, ff-only): backend auth **39 test xanh**, gateway permitAll 5 routes + cookie factory ADR D5, FE **47 test xanh** (tsc/lint/build green), pages login 2FA / reset-password / verify-email / account + BootstrapAuth. **Deferred minors (final review, sẽ triage trong phase sau):** xem ledger `.superpowers/sdd/2026-08-30-auth-completion/progress.md` (Task 1 _When_ casing/EOF newlines, Task 2 QR magic-byte/null-check, Task 3 /me divergence, Task 5 Redis-tx ordering, Task 6 dead stub, Task 7 baseUrl prod, Task 10 3 untested void wrappers, Task 11 password trim asym, Task 12 useVerify2faLogin store flags, Task 16 2FA-enable store refresh).
11. ✅ **Smoke-test tooling (2026-08-31→09-01)** — `scripts/totp.mjs` (TOTP RFC 6238, `node --test scripts/totp.test.mjs` 6/6), `scripts/smoke-auth.sh` (subcommand `boot|run|stop|all`: docker-compose + Mailpit + cài common-lib + 4 service + gateway qua python-spawn, poll readiness, chạy full flow A1-A8/B1/C1 qua cổng 9000 — register→verify-email (link từ Mailpit)→2FA enroll/TOTP→logout→login mfaRequired→verify-login→refresh + forgot/reset + dữ liệu thật playlists/tracks/search/audio Range 206), Mailpit trong compose, `application.yml` starttls env-overridable. **✅ Đã chạy full-stack runtime 2026-09-01 — `smoke-auth.sh run` PASS=35 FAIL=0** + full Playwright E2E 4/4 xanh. Ghi chú: `RateLimitingFilter` Bucket4j cứng 10/phút/IP → làm configurable `app.security.rate-limit-per-minute` (smoke/E2E set `APP_SECURITY_RATE_LIMIT_PER_MINUTE=100` vì full suite gộp nhiều register+login cùng IP); `/me` fix single-wrap. Backend `mvn test` xanh.
12. (Sau) user-service; track upload thật qua UI/FE + CDN/auth-cache cho streaming

## Cách dùng
- **AI:** đọc file này khi bắt đầu mọi task/session; khi kết thúc milestone → **cập nhật** giai đoạn + inventory + next actions.
- **Con người:** duyệt "Next actions", sửa trực tiếp file khi rẽ hướng.