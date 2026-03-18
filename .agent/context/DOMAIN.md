# Spotify Clone - Global Domain Map

This document bridges the gap between microservices. For detailed domain logic, refer to the **Local Context** inside each service folder.

## Bounded Context Registry

| Service | Responsibility | Local Context Location |
| :--- | :--- | :--- |
| **Auth** | User identity, password security | `com/spotify/auth/.agent-context.md` |
| **Playlist** | Track ordering, collections | `com/spotify/playlist/.agent-context.md` |
| **Track** (Backlog) | Upload, streaming, metadata | `com/spotify/track/.agent-context.md` |
| **Search** (Backlog) | Elasticsearch indexing | `com/spotify/search/.agent-context.md` |

## Global Cross-Cutting Concerns

- **ID Generation**: UUID (v4) used universally to ensure unique IDs across service boundaries.
- **Audit Standard**: All main entities must track `createdAt` and `updatedAt` (TIMESTAMPTZ).
- **Domain Purity**: Domain POJOs must never import Spring, JPA, or external frameworks.

## Event Communication Flow (Kafka)

| Subject | Action | From Service | To Service |
| :--- | :--- | :--- | :--- |
| `User` | `Registered` | `auth-service` | `user-service`, `email-service` |
| `Track` | `Uploaded` | `track-service` (Backlog) | `search-service`, `notify-service` |
| `Playlist` | `TrackMoved` | `playlist-service` | `search-service` |
| `Playlist` | `TrackAdded` | `playlist-service` (Backlog) | `search-service` |

---

> **Note for AI:** When tasks involve specific domain logic, ALWAYS read the corresponding `Local Context` file from the table above before making code changes.