# Project Status — Spotify Clone

> **Tài liệu sống (single source of truth) cho trạng thái dự án.**
> Cập nhật khi xong milestone: giai đoạn, inventory, next actions. Dữ liệu ngày: **2026-08-29**.
> Trước khi bắt tay bất kỳ task nào, đọc file này (theo `CLAUDE.md`) để nắm tổng thể FE/BE.

## Giai đoạn hiện tại

**Frontend UI build (mock data) + Backend auth/playlist khung.**

| Phía | Trạng thái |
|---|---|
| **Frontend** | Đang xây UI theo Figma: Home, Playlist, Header-search, Player, token system, unified scrollbar. Data **mock** (`lib/musicData.ts` → `TRACK_INDEX`), auth nối thật qua Gateway. Chưa nối API data thật. |
| **Backend** | `auth` (JWT + OAuth2 Google qua Gateway, login/logout/register, Kafka basic) + `playlist` khung (Clean Arch đầy đủ tầng). `user` / `track` / `search` service vẫn **Backlog** (xem `domain.md`). |
| **Gateway** | Spring Cloud Gateway — route + JWT validation filter (certs). |

## Frontend inventory

### Routes (`frontend/app/`)
- `/` — Home feed (container-query grid)
- `/playlist/[id]` — màn playlist (demo ngay cả khi chưa login)
- `/login`, `/register`, `/forgot-password`, `/oauth2/callback`
- `error.tsx` / `loading.tsx` / `not-found.tsx` — App Router chuẩn
- **Không còn route `/search`** — SearchBar sống trên header TopNav

### Components
- `MainLayout` — 3 panel + drag-resize sidebar (`--left-w`/`--right-w`, localStorage)
- `TopNav` — chevron back/forward + `SearchBar` giữa + avatar dropdown (Account/Settings/Logout)
- `LibraryNav` — Home / Your Library + danh sách playlist (scroll)
- `HomeFeed` + `MusicCard` (cva, memo), `SectionHeader`
- `FriendActivity` — panel phải
- `Player` + `player/` (TrackInfo, PlaybackControls, PlayerProgress, VolumeControl, ControlButton)
- `playlist/` (TrackTable, TrackRow + context menu)
- `search/` (SearchBar combobox, SearchResultRow, SearchSuggestionRow)

### Design system
- `tokens.css` + `@theme inline` trong `globals.css` → utility `bg-*/text-*/border-*` (token-only, cấm arbitrary-hex)
- **Unified scrollbar** (global: webkit + Firefox) + `@utility no-scrollbar`
- State: Zustand (`usePlayerStore`, `useAuthStore`) — **granular selectors**; React Query (`queryKeys.ts`)

### Services & tests
- `services/search/searchService.ts` — logic search thuần (mock `TRACK_INDEX`), đã TDD
- `services/api/authService.ts` — auth qua Gateway
- Vitest: **26 test xanh** (searchService 11, SearchBar 8, PlayerProgress, usePlayerStore)

## Backend inventory

### `backend/` — **Maven multi-module** (chuyển từ monolith → microservices, 2026-08-29)
- `common-lib` — `ApiResponse`, `GlobalResponseWrapper`, `GatewayHeaderFilter`, `ServiceSecurityConfig` (jar chia sẻ)
- `auth-service` (port **8081**) — Clean Arch: domain / application (usecase, port) / infrastructure (persistence, security **oauth2**, **TOTP 2FA**, messaging **Kafka**, Redis session) / presentation (controller)
- `playlist-service` (port **8084**) — Clean Arch: domain (entity, **LexoRankService** thuần, event) / application (usecase) / infrastructure (persistence adapter/mapper) / presentation (controller)
- **Database-per-service:** `auth_db` (5432) + `playlist_db` (5433) trong `docker-compose.yml` (migrations consolidated 2026-08-29: mỗi service 1 file `V1__init_*.sql`, đánh số riêng theo service — auth gộp V1-V4,V6 + bỏ bảng dead `security_tokens`; playlist đổi tên V5 → V1)
- Infra môi trường: `docker-compose.yml` (2×Postgres/Redis/Kafka/kafka-ui), `.env`
- `common` cũ: `GlobalExceptionHandler` + auth exception → về auth-service (common cũ từng import ngược auth — đã gỡ phụ thuộc)

### `gateway/` — Spring Cloud Gateway
- Route config + JWT validation filter, certs

### Skills / workflow
- **`be-workflow`** (`.claude/skills/be-workflow/`) — pipeline bắt buộc cho task BE: 4 bước + 2 gate, verify `./mvnw test` + Clean Arch self-check + review độc lập.
- **`fe-workflow`** — pipeline FE tương ứng.
- **Plugin cộng đồng đã cài: `engineering-advanced-skills@claude-code-skills`** (2.9.0) — `migration-architect`, `database-designer`, `api-design-reviewer`, `api-test-suite-builder`, `pr-review-expert`, `ship-gate`… dùng qua be-workflow bước 3/4.
- **Đã bỏ 3 skill tự viết cũ** (`db-migration`/`kafka-event`/`security-review`) — không có SKILL.md; thay bằng skill cộng đồng. Kafka giữ inline theo `domain.md`.

### Backlog (theo `domain.md`)
- `user-service` (profile, follows), `track-service` (upload/stream/metadata), `search-service` (Elasticsearch)

## Next actions (dự kiến — để chủ dự án duyệt)
1. ✅ Tạo `be-workflow` + cài plugin `engineering-advanced-skills` + bỏ skill cũ (2026-08-29)
2. ✅ Tách backend monolith → microservices (auth 8081, playlist 8082, common-lib, database-per-service) (2026-08-29) — build xanh 17 test, gateway retarget xong
3. Chạy stack thật (docker-compose + 2 service + gateway) → **smoke test E2E qua cổng 9000**
4. Nợ kỹ thuật đã phát hiện: `RebalancePlaylistUseCaseImpl` import JPA entity vào application (vi phạm Clean Arch); `playlistLogDomainEventPublisher` tên class viết thường
5. Pick service backend tiếp theo (theo backlog) — hoặc tiếp tục UI FE mới từ Figma
6. Nối FE sang API thật (thay mock `TRACK_INDEX` bằng `/api/v1/...`)
7. Bổ sung test backend (hiện 17 test: auth 13, playlist 4)

## Cách dùng
- **AI:** đọc file này khi bắt đầu mọi task/session; khi kết thúc milestone → **cập nhật** giai đoạn + inventory + next actions.
- **Con người:** duyệt "Next actions", sửa trực tiếp file khi rẽ hướng.