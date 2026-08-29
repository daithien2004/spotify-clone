# .claude/ Directory Structure

This directory contains Claude Code configuration, rules, skills, agents, commands, and hooks for the Spotify Clone project.

## Directories

- **agents/** - Custom agents invokable via `/agent <name>` for complex, multi-step tasks
- **commands/** - Custom slash commands (hiện rỗng — command `/vercel-react` đã xóa 2026-08-29, skill `vercel-react-best-practices` load trực tiếp qua `skill:`)
- **hooks/** - Hook scripts that run automatically before/after certain tool operations
- **rules/** - Project-specific guidelines and standards (source of truth)
- **skills/** - Reusable skill implementations for common tasks
- **memory.md** - Project memory & context (persists across sessions)
- **settings.json** - Permissions & hooks configuration
- **settings.local.json** - Local overrides (RTK permissions, API key)

## Usage

### Agents
Invoke with: `/agent <agent-name>`
Example: `/agent feature-agent`

### Commands  
Invoke with: `/<command-name>`

### Skills
Invoke with: `skill: "<skill-name>"`
Example: `skill: "be-workflow"`

### Hooks
Automatically triggered based on matcher patterns in settings.json

## File Naming Conventions

- Agents: `{name}-agent.md` (e.g., `feature-agent.md`)
- Commands: `{name}.md` (hiện thư mục rỗng — trước có `db-migration.md`, `kafka-event.md`, `security-review.md`, `vercel-react.md`, đã xóa 2026-08-29; logic chuyển sang skill `{name}/SKILL.md`)
- Hooks: Descriptive names (e.g., `post-lint-fix.sh`)
- Skills: `{name}/SKILL.md` (e.g., `be-workflow/SKILL.md`)
- Rules: `{topic}.md` (e.g., `context.md`)