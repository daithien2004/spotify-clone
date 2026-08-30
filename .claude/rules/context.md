# Spotify Clone - Context

- Tên dự án: Spotify Clone
- Backend: Java 21, Spring Boot 3, Clean Architecture
- Frontend: Next.js 14 App Router, TypeScript, Tailwind CSS, React Query, Zustand
- Message broker: Apache Kafka
- Databases: PostgreSQL (main), Redis (cache/session), Elasticsearch (search)
- Storage: MinIO (audio files, artwork)
- Vector DB: Pinecone (track embeddings cho recommendation)
- Container: Docker + Kubernetes

## Clean Architecture Layers (Backend)
- `domain/`: Entities, Repository interfaces, Domain Services, Domain Events — NO Spring imports
- `application/`: Use Cases, Input/Output Ports, Application Services — imports `domain` only
- `infrastructure/`: JPA Entities, Repository Implementations, Kafka producers/consumers, External API clients — implements domain interfaces
- `presentation/`: Controllers, Request/Response DTOs, Exception Handlers — calls Use Cases only

## Microservices (monorepo — Maven multi-module)
Repos vật lý: `backend/pom.xml` (parent) → `common-lib`, `auth-service`, `playlist-service`, `track-service`, `search-service`. Gateway (`gateway/`, port 9000) route tới từng service (auth 8081, playlist 8084, track 8085, search 8086). Database-per-service: `auth_db` (5432) / `playlist_db` (5433) / `track_db` (5434) trong `backend/docker-compose.yml` (+ MinIO :9010, Elasticsearch :9200, kafka-ui :8087).

| Service | Status | Responsibility |
|---|---|---|
| `auth-service` | ✅ `backend/auth-service/` | Authentication, JWT, OAuth2, TOTP 2FA |
| `playlist-service` | ✅ `backend/playlist-service/` (port **8084**) | Playlist metadata + track ordering (LexoRank), add/get/reorder/rebalance |
| `common-lib` | ✅ `backend/common-lib/` | ApiResponse envelope, GatewayHeaderFilter, ServiceSecurityConfig |
| `user-service` | 🔴 Backlog | User profiles, follows |
| `track-service` | ✅ `backend/track-service/` (port **8085**) | Track catalog metadata CRUD + batch GET + **audio upload/streaming (MinIO, HTTP byte-range)** + publish Track events (Kafka) |
| `search-service` | ✅ `backend/search-service/` (port **8086**) | Elasticsearch full-text search: index `tracks`, Kafka consumer + bootstrap reindex, `GET /api/v1/search/tracks` |

## Technical Decisions (Đã chốt — không thay đổi)
- **Use Case per feature:** Không dùng god-class Service.
- **Domain entity ≠ JPA entity:** Map riêng tại infrastructure layer.
- **Event-driven:** Cross-service communication qua Kafka Domain Events.
- **API Gateway:** Spring Cloud Gateway là single entry point.
- **Auth:** JWT stateless + Redis để blacklist token khi logout.

## AI Instructions
- Hỏi rõ service nào trước khi tạo file mới.
- Không tự thêm dependency ngoài stack đã chốt ở trên.
- Không phá vỡ Clean Architecture layer boundaries.
