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

## Microservices
| Service | Responsibility |
|---|---|
| `auth-service` | Authentication, JWT, OAuth2 |
| `user-service` | User profiles, follows |
| `track-service` | Upload, streaming, metadata |
| `playlist-service` | Playlist CRUD, track ordering |
| `search-service` | Elasticsearch, full-text search |

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