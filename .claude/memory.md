# Claude Memory

> Pointer cho trạng thái/tiến độ dự án → **xem `PROJECT_STATUS.md`** (repo root) — đây là single source of truth, cập nhật khi xong milestone.

## Preference (theo project CLAUDE.md / rules)
- Ngôn ngữ trao đổi: Tiếng Việt
- Coding style: Clean Architecture (backend), token-only + component chuẩn (frontend), TDD khi có logic
- Frontend: Next.js App Router, TypeScript, Tailwind v4, Zustand, React Query, vitest
- Backend: Java 21, Spring Boot 3, Kafka, PostgreSQL/Redis/Elasticsearch
- Mọi task chạm `frontend/` phải chạy **fe-workflow** (2 gate duyệt)
- Mọi task chạm `backend/`/`gateway/` phải chạy **be-workflow** (2 gate duyệt, verify `./mvnw test`)
- Mọi thay đổi file dùng **Edit/Write** (Bash write bị hook chặn)

## Session tips
- Bắt đầu session: đọc `CLAUDE.md` + `.claude/rules/` + `PROJECT_STATUS.md`
- Khám phá code: dùng codebase-memory MCP + fallback Grep/Glob/Read