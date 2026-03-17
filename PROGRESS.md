# Progress tracker — Spotify Clone

## Done
- [x] Initialized Antigravity Agent Guidelines, Skills & Workflows theo chuẩn Clean Architecture.

## In Progress
| Feature | Service | % | Notes |
|---------|---------|---|-------|
| Setup Cơ Sở | All | 100% | Đã nạp file cấu trúc cơ sở dự án (`CONTEXT.md`, `DOMAIN.md`, `CONVENTIONS.md`) |
| Giao Diện Frontend | Next.js | 20% | Đã có trang chủ (`page.tsx`) với giao diện dummy playlists |
| Authentication | auth-service | 50% | Đã thiết lập `AuthController` với endpoints Login/Register cơ bản, `User` entity |

## Next Up (backlog theo priority)
1. Cấu hình bảo mật JWT và hoàn thiện service `auth-service`.
2. Khởi tạo `track-service`.

## Known Issues
| ID | Description | Severity | Layer |
|----|-------------|----------|-------|
| - | - | - | - |

## Architecture Decisions (ADR)
### ADR-001: Domain Entity ≠ JPA Entity
- Context: Isolation of Domain Logic from Framework persistence details.
- Decision: Use separate classes for Domain vs JPA. Implement mapping in the infrastructure layer.
- Consequences: Extra mapping code but cleaner domain layer and framework independence.
