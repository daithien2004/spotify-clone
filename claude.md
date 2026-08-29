# Spotify Clone - Claude Code Project Instructions

## Quick Reference
**BEFORE any task:** Read `.claude/rules/` files (except `HEALTH.md`—see health process if needed) **and `PROJECT_STATUS.md`** (trạng thái/giai đoạn dự án + inventory FE/BE).  
Cập nhật `PROJECT_STATUS.md` khi xong milestone (đổi giai đoạn/inventory/next actions).  
Also review global instructions in `~/.claude/CLAUDE.md` and `RTK.md` for tool-specific guidance.

## Editing Files — Use the Tools, Not Bash (Enforced)

**Đọc/create/sửa file CHỈ bằng `Edit` / `Write`.** Không bao giờ dùng Bash để ghi đè file:

| ❌ Bash pattern (bị chặn) | ✅ Thay bằng |
|---|---|
| `sed -i 's/a/b/' file` | `Edit` |
| `sed 's/a/b/' file > /tmp/x.tmp` + `mv` | `Edit` |
| `cat <<EOF > file` (heredoc) | `Write` |
| `cat x > file` / `printf ... > file` | `Write` |
| `echo '...' > file` | `Write` |
| `mv /tmp/*.tmp file` | `Write` |

- Hook `PreToolUse` (`.claude/hooks/block-bash-file-edit.sh`) chặn cứng các pattern trên ở tầng cơ chế — lệnh bị deny kèm reason, không phụ thuộc vào việc model "tự giác". Chọn `Edit`/`Write` ngay từ đầu để khỏi bị chặn rồi làm lại.
- **Vẫn được phép:** append log (`> log >> file` kiểu `>>`), redirect `/dev/null`, bash đọc-only (`grep`, `sed -n` preview, `find`, `git`).

---

## Project Structure

```
spotify-clone/
├── .claude/           # Claude Code config, rules, skills
├── backend/           # Maven multi-module (parent POM)
│   ├── common-lib/        # shared: ApiResponse, GatewayHeaderFilter, ServiceSecurityConfig
│   ├── auth-service/      # Spring Boot app (8081) — JWT/OAuth2/2FA/account security
│   └── playlist-service/  # Spring Boot app (8084) — track ordering, collections
├── gateway/           # Spring Cloud Gateway (9000) — routes to 8081/8084
└── frontend/          # Next.js React application
```

> **Kiến trúc 2026-08-29:** backend chuyển từ monolith → **microservices** (database-per-service:
> auth_db / playlist_db trong docker-compose). `user`/`track`/`search` service vẫn **Backlog** (`domain.md`).

## Rules (Source of Truth)

| File | Purpose |
|------|---------|
| `.claude/rules/context.md` | Tech stack, microservices, Clean Architecture |
| `.claude/rules/conventions.md` | Coding standards, naming, patterns |
| `.claude/rules/craftsman.md` | Quality standards, TDD, DDD |
| `.claude/rules/domain.md` | Global domain map, Kafka events |

## Project-Specific Skills

| Skill | When to Use |
|-------|-------------|
| `be-workflow` | **Bắt buộc mọi task backend** (`backend/`/`gateway/`) — 4 bước 2 gate |
| `fe-workflow` | **Bắt buộc mọi task frontend** (`frontend/`) — 4 bước 2 gate |
| `migration-architect` `database-designer` | New database tables / schema change (plugin `engineering-advanced-skills`) |
| `api-design-reviewer` `api-test-suite-builder` | = Before/after implementing endpoints (cùng plugin) |
| `pr-review-expert` `ship-gate` | Independent review / pre-production audit |
| `vercel-react-best-practices` | Before implementing frontend (skill thật từ `vercel-labs/agent-skills`) |

Ghi chú: 3 skill tự viết cũ `db-migration`/`kafka-event`/`security-review` + command `/vercel-react` đã xóa (2026-08-29) — thay bằng skill cộng đồng đã cài (plugin `engineering-advanced-skills@claude-code-skills`). Kafka: giữ inline theo `domain.md` event map.

## Build Commands
### Frontend
```bash
cd frontend && npm run dev      # development server
cd frontend && npm run build    # production build
cd frontend && npm run lint     # code linting
```

### Backend (multi-module)
```bash
cd backend && ./mvnw test                     # run all module tests (auth + playlist)
cd backend && ./mvnw clean package            # package all services
cd backend && ./mvnw -pl auth-service spring-boot:run      # run auth-service (8081)
cd backend && ./mvnw -pl playlist-service spring-boot:run  # run playlist-service (8084)
```
> **Lưu ý Windows:** nếu `./mvnw` (bash) báo `ClassNotFoundException`, dùng `mvnw.cmd`
> (Maven dist 3.9.12 đã cached tại `~/.m2/wrapper/dists/`).