# Spotify Clone - Agent Guidelines

## Quick Reference

**BEFORE any task:** Read `.agent/context/` files (except `HEALTH.md`).

---

## Project Structure

```
spotify-clone/
├── .agent/           # Agent-specific context, skills, workflows
├── backend/          # Java Spring Boot API
└── frontend/         # Next.js React application
```

## Agent Context (Source of Truth)

| File | Purpose |
|------|---------|
| `.agent/context/CONTEXT.md` | Tech stack, microservices, Clean Architecture |
| `.agent/context/CONVENTIONS.md` | Coding standards, naming, patterns |
| `.agent/context/CRAFTSMAN.md` | Quality standards, TDD, DDD |
| `.agent/context/DOMAIN.md` | Global domain map, Kafka events |
| `.agent/context/HEALTH.md` | Security, bugs, tech debt |

## Project-Specific Skills

| Skill | When to Use |
|-------|-------------|
| `.agent/skills/db-migration/` | New database tables |
| `.agent/skills/kafka-event/` | Cross-service communication |
| `.agent/skills/security-review/` | Before implementing endpoints |
| `.agent/skills/vercel-react-best-practices/` | Before implementing frontend |

## Build Commands

```bash
# Frontend
cd frontend && npm run dev | build | lint

# Backend
cd backend && ./mvnw spring-boot:run | test | clean package
```
