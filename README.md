# Spotify Clone — Microservices Full-Stack

A full-stack music streaming application inspired by Spotify, built to practice **Java Spring Boot microservices**, **Clean Architecture**, and **AI-assisted development workflows**.

## Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language / Runtime | Java 21, Spring Boot 3.2 |
| Architecture | Microservices + Clean Architecture (domain / application / infrastructure / presentation) |
| API Gateway | Spring Cloud Gateway (JWT validation filter, route-level auth) |
| Auth | JWT (access + refresh cookie), OAuth2 Google, TOTP 2FA (RFC 6238), Bcrypt |
| Messaging | Apache Kafka (KRaft mode) — event-driven index sync between track & search service |
| Search | Elasticsearch 8.15 — multi-field fuzzy search (title / artist / album) |
| Object Storage | MinIO — audio file upload + HTTP byte-range streaming |
| Database | PostgreSQL 16 (database-per-service), Flyway migrations |
| Cache / Rate-limit | Redis 7 — refresh token, MFA token (single-use), Bucket4j rate limiter |
| Email | JavaMailSender + Mailpit (dev SMTP sink) |
| Build | Maven multi-module |

### Frontend
| Layer | Technology |
|---|---|
| Framework | Next.js 16 (App Router), React 19, TypeScript |
| State | Zustand (player + auth store, granular selectors), TanStack React Query |
| Styling | Tailwind CSS v4, design token system (tokens.css) |
| HTTP | Axios + custom API client (envelope unwrap, silent refresh-token flow) |
| Testing | Vitest + Testing Library (47 unit tests), Playwright E2E |

---

## Architecture Overview

```
Browser
  |
  +-> Next.js Frontend (port 3000)
          |  /api/v1/* proxy
          v
   Spring Cloud Gateway (port 9000)
   +-- JWT validation filter (public routes bypass)
   +-- /auth/**      -> auth-service     :8081
   +-- /playlists/** -> playlist-service :8084
   +-- /tracks/**    -> track-service    :8085
   +-- /search/**    -> search-service   :8086

Infrastructure (docker-compose):
  PostgreSQL x3  (auth_db :5432 | playlist_db :5433 | track_db :5434)
  Redis          :6379
  Kafka (KRaft)  :9092
  Kafka UI       :8087
  Elasticsearch  :9200
  MinIO          :9010  (bucket: tracks)
  Mailpit        :8025  (SMTP :1025)
```

---

## Services

### `auth-service` (port 8081)
- Register / Login / Logout with **JWT access + refresh token** (HttpOnly cookie)
- **OAuth2 social login** (Google) via Spring Security
- **TOTP Two-Factor Authentication** — enroll (QR code), verify setup, 2FA login flow, disable; brute-force guard via Redis counter
- Forgot / Reset password via email link
- Email verification with auto-send on register
- `PATCH /me` profile update (display name, avatar URL)
- **39 unit tests** (usecase layer, domain model, TOTP adapter)

### `playlist-service` (port 8084)
- Playlist CRUD with **LexoRank ordering** (full 63-char charset)
- Add / reorder tracks; async rebalance on rank exhaustion (`PlaylistRebalanceScheduler`)
- **27 unit tests** (LexoRank algorithm, scheduler, add/reorder usecases)

### `track-service` (port 8085)
- Track metadata CRUD + **batch GET** (`GET /tracks?ids=a,b,c` — preserves input order)
- **Audio upload -> MinIO** (`PUT /tracks/{id}/audio` multipart)
- **HTTP byte-range streaming** (`GET /tracks/{id}/audio`, `Accept-Ranges`, seekable in browser)
- Kafka event publisher (`spotify.track.events`) — fan-out to search-service
- **26 unit tests**

### `search-service` (port 8086)
- Elasticsearch index `tracks` — multi-match + fuzziness AUTO (title^3, artist^2, album)
- `GET /search/tracks?q=&limit=` endpoint
- **Kafka consumer** — real-time index sync on track create / update / delete
- **Bootstrap reindex** on startup (fetches all tracks from track-service)
- **16 unit tests**

### `gateway` (port 9000)
- JWT validation filter (RS256); public routes bypass (forgot-password, reset-password, email verification, 2FA verify-login)

### `common-lib`
- `ApiResponse<T>` envelope + `GlobalResponseWrapper` (AOP)
- `GatewayHeaderFilter` — injects verified user identity header downstream
- Kafka event contracts: `TrackEventEnvelope`, `TrackPayload`

---

## Frontend Features

- **Spotify-like layout** — resizable 3-panel (sidebar / main / activity), drag-resize persisted to localStorage
- **Playlist page** — real data from playlist-service + track-service batch GET, joined by LexoRank
- **HTML5 Audio Player** — streams from MinIO via Gateway, byte-range seek, duration from metadata, queue/next/previous
- **SearchBar** — live search via Elasticsearch (debounce 300ms, combobox suggestions)
- **Auth pages**: Login (with TOTP step), Register, Forgot/Reset Password, Email Verify, `/account` (profile + 2FA management)
- **Middleware gating** — protected routes redirect to `/login` when no `auth-token` cookie
- **Silent refresh** — axios interceptor queues 401s, refreshes token, replays requests
- **47 unit tests** + **Playwright E2E** (gating + full-stack auth: register → verify email via Mailpit → login → 2FA)

---

## Running Locally

### Prerequisites
- Java 21, Maven 3.9+
- Node.js 20+, npm
- Docker Desktop

### 1. Start infrastructure

```bash
cd backend
cp .env.example .env      # fill in DB_PASSWORD and secrets
docker compose up -d
```

### 2. Build common-lib

```bash
cd backend
./mvnw install -pl common-lib -am -DskipTests
```

### 3. Start backend services

```bash
cd backend/auth-service     && ../mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd backend/playlist-service && ../mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd backend/track-service    && ../mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd backend/search-service   && ../mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd gateway                  && ./mvnw spring-boot:run
```

### 4. Start frontend

```bash
cd frontend
npm install
npm run dev     # http://localhost:3000
```

### 5. Run tests

```bash
# Backend (all modules)
cd backend && ./mvnw test

# Frontend unit tests
cd frontend && npm test

# Playwright E2E (gating — FE only)
cd frontend && npm run test:e2e

# Playwright full-stack (requires running stack + Docker)
cd frontend && E2E_FULL=1 npm run test:e2e
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| **Database-per-service** | Each service owns its schema; no shared DB coupling |
| **LexoRank ordering** | O(1) reorder without renumbering entire list; auto-rebalance on exhaustion |
| **Kafka for search index sync** | Decouples track-service from search-service; bootstrap reindex handles cold-start |
| **HTTP byte-range streaming** | Browser native seek without downloading the entire audio file |
| **TOTP 2FA with single-use Redis token** | MFA token deleted on first use (TTL 5 min) to prevent replay attacks |
| **Clean Architecture per service** | Domain logic is framework-agnostic and testable without Spring context |
| **common-lib shared jar** | Avoids duplicating API envelope, security config, and Kafka contracts |

---

## Test Summary

| Module | Tests |
|---|---|
| common-lib | 5 |
| auth-service | 39 |
| playlist-service | 27 |
| track-service | 26 |
| search-service | 16 |
| **Backend total** | **113** |
| Frontend (Vitest) | 47 |
| Playwright E2E | 2+ |

---

## Project Structure

```
spotify-clone/
+-- backend/                        # Maven multi-module
|   +-- common-lib/                 # Shared: ApiResponse, security config, Kafka contracts
|   +-- auth-service/               # JWT + OAuth2 + TOTP 2FA
|   +-- playlist-service/           # Playlist + LexoRank ordering
|   +-- track-service/              # Track catalog + MinIO audio streaming
|   +-- search-service/             # Elasticsearch + Kafka consumer
|   +-- docker-compose.yml          # Full infra stack
+-- gateway/                        # Spring Cloud Gateway
+-- frontend/                       # Next.js 16 App Router
|   +-- app/                        # Pages & layouts
|   +-- components/                 # UI components (Player, SearchBar, etc.)
|   +-- hooks/                      # useAuth, usePlayerStore, useAuthStore
|   +-- services/api/               # API clients (auth, playlist, track, search)
|   +-- lib/                        # api-client, adapters, validation
|   +-- e2e/                        # Playwright tests
+-- scripts/
    +-- totp.mjs                    # TOTP RFC 6238 helper (Node.js)
    +-- smoke-auth.sh               # Full-stack smoke test script
```
