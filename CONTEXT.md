# Spotify Clone - Context

- Tên dự án: Spotify Clone
- Backend: Java 21, Spring Boot 3, Clean Architecture
- Frontend: Next.js 14 App Router, TypeScript, Tailwind CSS, React Query, Zustand
- Message broker: Apache Kafka
- Databases: PostgreSQL (main), Redis (cache/session), Elasticsearch (search)
- Storage: MinIO (audio files, artwork)
- Vector DB: Pinecone (track embeddings cho recommendation)
- Container: Docker + Kubernetes

## Clean Architecture layers (backend):
- `domain/`: Entities, Repository interfaces, Domain Services, Domain Events — KHÔNG import Spring
- `application/`: Use Cases, Input/Output Ports, Application Services — import `domain` only
- `infrastructure/`: JPA Entities, Repository Implementations, Kafka, External APIs — implements domain interfaces
- `presentation/`: Controllers, Request/Response DTOs, Exception Handlers — gọi Use Cases only

## Microservices: 
auth-service, user-service, track-service, playlist-service, search-service

## Quyết định kỹ thuật đã chốt:
- Dùng Use Case per feature (không service god class)
- Domain entity ≠ JPA entity (map riêng ở infrastructure)
- Event-driven cho cross-service communication qua Kafka
- API Gateway là single entry point (Spring Cloud Gateway)

> **Yêu cầu dành cho AI:** Luôn đọc và tuân thủ các quy tắc trong CONTEXT.md, DOMAIN.md, CONVENTIONS.md trước khi generate code. Không phá vỡ Clean Architecture.
