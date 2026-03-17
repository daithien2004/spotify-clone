# Spotify Clone - Conventions

Bao gồm các Standard Rules dành cho AI khi generate Code. Bắt buộc đọc trước khi xử lý logic/tạo file.

## 1. Clean Architecture - Backend
- **Dependency Rule:** mũi tên dependency hướng từ ngoài vào trong: `presentation → application → domain` và `infrastructure → domain`.
- **Tuyệt đối KHÔNG BAO GIỜ:** domain → application, domain → infrastructure, domain → Spring.

- **Package structure:** `com.spotify.{service-name}.{layer}.{subdomain}`
- **Class naming:**
  - Domain entity: `Track`, `User` (plain Java, no Spring)
  - Use Case: `{Feature}UseCase` (interface), `{Feature}UseCaseImpl`
  - Repository interface (domain): `TrackRepository`
  - Repository impl (infra): `JpaTrackRepository`, `TrackRepositoryAdapter`
  - JPA Entity (infra): `TrackJpaEntity`
  - Controller (presentation): `TrackController`
  - Request/Response DTO: `UploadTrackRequest`, `TrackResponse`
- **Exceptions:** Naming `TrackNotFoundException`.
- **Test naming:** `should_[expectedBehavior]_when_[condition]` (Ví dụ: `should_ThrowException_when_TrackDurationIsZero`).
- **Forbidden Patterns:**
  - `@Autowired` field injection → dùng constructor injection.
  - Business logic trong `@Controller`.
  - Spring annotations trong `domain/` layer.

## 2. API Contracts
- **URL pattern:** `/api/v1/{resource}[/{id}][/{sub-resource}]`
- **Response wrapper:** `ApiResponse<T> { status, data, message }`.
- **REST Status:** 200 (GET/PUT), 201 (POST), 204 (DELETE), 400 (Validation), 401, 403, 404.

## 3. Frontend Next.js 14 (App Router)
- **Folder structure:** `app/(features)/{feature-name}/`, không dùng Pages router.
- **Component naming:** PascalCase (`TrackCard.tsx`). Hooks camelCase (`useTrack.ts`).
- **State Management:**
  - Server state: **React Query** (`useQuery`, `useMutation`). KHÔNG dùng `useEffect` fetch data.
  - Global UI state: **Zustand** (`usePlayerStore.ts`).
- **Server Components:** Mặc định là Server Components. Chỉ dùng `'use client'` khi có useState/useEffect, event handlers hoặc Hooks React Query.

## 4. Git
- **Branch:** `feature/{ticket-id}-{short-description}`
- **Commit:** `{type}({scope}): {description}` (Ví dụ: `feat(track): add upload endpoint`)
