# Spotify Clone - Claude Code Project Instructions

## Quick Reference
**BEFORE any task:** Read `.claude/rules/` files (except `HEALTH.md`—see health process if needed).  
Also review global instructions in `~/.claude/CLAUDE.md` and `RTK.md` for tool-specific guidance.

---

## Project Structure

```
spotify-clone/
├── .claude/           # Claude Code config, rules, skills
├── backend/           # Java Spring Boot API
├── gateway/           # Spring Cloud Gateway
└── frontend/          # Next.js React application
```

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
| `db-migration` | New database tables |
| `kafka-event` | Cross-service communication |
| `security-review` | Before implementing endpoints |
| `vercel-react-best-practices` | Before implementing frontend |

## Build Commands
### Frontend
```bash
cd frontend && npm run dev      # development server
cd frontend && npm run build    # production build
cd frontend && npm run lint     # code linting
```

### Backend
```bash
cd backend && ./mvnw spring-boot:run  # run application
cd backend && ./mvnw test             # run tests
cd backend && ./mvnw clean package    # package artifacts
```