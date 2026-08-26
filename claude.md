# Spotify Clone - Claude Code Project Instructions

## Quick Reference

**BEFORE any task:** Read `.claude/rules/` files (except `HEALTH.md`).

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

```bash
# Frontend
cd frontend && npm run dev | build | lint

# Backend
cd backend && ./mvnw spring-boot:run | test | clean package
```
