# Spotify Clone - Conventions

Bao gồm các Standard Rules dành cho AI khi generate Code. Bắt buộc đọc trước khi xử lý logic/tạo file.

## 1. Clean Architecture - Backend
- **Dependency Rule:** Dependencies only flow inward: `presentation → application → domain` and `infrastructure → domain`.
- **Isolation:** Domain layer must NEVER import Spring, JPA, or any external framework.
- **Method Scope:** Prioritize small, focused methods (< 40 lines). Use **Early Returns** to reduce nested blocks.
- **SOLID:** Adhere to SOLID, DRY, KISS, YAGNI principles throughout.

- **Source File Structure:** (Google Style) License → Package → Imports (no wildcards) → Top-level Class.
- **Formatting:** 2-space indentation (Google standard), 100-character column limit.
- **Naming Conventions:**
  - Packages: lowercase, no underscores (`com.spotify.auth.domain`).
  - Classes/Interfaces: `PascalCase` (`RegisterUseCase`).
  - Methods/Variables: `camelCase` (`executeTask`).
  - Constants: `CONSTANT_CASE`.
  - Tests: `should_[ExpectedBehavior]_when_[Condition]` (e.g. `should_ThrowException_when_TrackDurationIsZero`).
- **Forbidden Patterns:**
  - `@Autowired` field injection → use constructor injection.
  - Business logic inside `@Controller`.
  - Spring/JPA annotations inside `domain/` layer.
- **Logging:** Use SLF4J with Logback. Proper log levels: ERROR, WARN, INFO, DEBUG.
- **DB Migrations:** Use Flyway or Liquibase — never modify schema manually.
- **API Docs:** Annotate all endpoints with Springdoc OpenAPI (Swagger).

## 2. API Contracts
- **Design Philosophy:** (Stripe-inspired) Resource-oriented, predictable URLs, and idempotent operations.
- **URL Pattern:** `/api/v1/{resource}[/{id}][/{sub-resource}]`.
- **Inbound Data:** Use `@Valid` and Bean Validation. Prefer `record` for DTOs.
- **Idempotency:** For critical mutations (POST), support `X-Idempotency-Key` headers.
- **Error Handling:** Standardized `ApiResponse<T>` with HTTP-appropriate status codes (200, 201, 204, 400, 401, 403, 404, 500).
- **Consistency:** Use `@ControllerAdvice` for global exception mapping.
- **Security:** Use BCrypt for password encoding. Configure CORS explicitly — never wildcard in production.

## 3. Frontend Next.js 14 (App Router)
- **Folder structure:** `app/(features)/{feature-name}/`. Never use Pages Router.
- **Component naming:** PascalCase (`TrackCard.tsx`). Hooks: camelCase (`useTrack.ts`).
- **TypeScript:** Strict mode required. No `any`. Prefer `interface` for object shapes, `type` for unions/primitives.
- **State Management:**
  - Server state: **React Query** (`useQuery`, `useMutation`). NEVER use `useEffect` to fetch data.
  - Global UI state: **Zustand** (`usePlayerStore.ts`).
- **Server vs Client Components:**
  - Default to Server Components. Fetch data directly with `async/await` in Server Components.
  - Only add `'use client'` when needed: `useState`/`useEffect`, event handlers, React Query hooks.
- **Error Handling:** Use `error.tsx` for route errors, `not-found.tsx` for 404s — per App Router convention.
- **Performance:** Use `next/image` for all images. Use `next/dynamic` for heavy Client Components.
- **Query Keys:** Array format `['tracks', trackId]`, centralized in `queryKeys.ts`.
- **Services layer:** Functions in `services/` must throw user-friendly errors that React Query can catch and display.

## 4. Git
- **Branch:** `feature/{ticket-id}-{short-description}`
- **Commit:** `{type}({scope}): {description}` (e.g. `feat(track): add upload endpoint`)

## 5. Health Maintenance (HEALTH.md)
- **Incremental Health Check**: Không quét toàn bộ dự án. Chỉ kiểm tra kỹ các file đang trực tiếp chỉnh sửa và dependencies liên quan.
- **Categorization**: 
  - `SEC-xxx`: Security (Data, Auth, Privacy).
  - `BUG-xxx`: Functional bugs.
  - `CLN-xxx`: Clean Architecture violations & Tech Debt.
- **Traceability**: Mỗi item phải có `Service` và `Status` (Open, In Progress, Fixed).
- **Cleanup**: Chỉ xóa item sau khi đã verify xong bằng automated tests.