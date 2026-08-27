# .claude/ Directory Structure

This directory contains Claude Code configuration, rules, skills, agents, commands, and hooks for the Spotify Clone project.

## Directories

- **agents/** - Custom agents invokable via `/agent <name>` for complex, multi-step tasks
- **commands/** - Custom slash commands (like `/db-migration`, `/kafka-event`) for easier skill invocation
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
Example: `/db-migration create tracks`

### Skills
Invoke with: `skill: "<skill-name>"`
Example: `skill: "db-migration"`

### Hooks
Automatically triggered based on matcher patterns in settings.json

## File Naming Conventions

- Agents: `{name}-agent.md` (e.g., `feature-agent.md`)
- Commands: `{name}.md` (e.g., `db-migration.md`)
- Hooks: Descriptive names (e.g., `post-lint-fix.sh`)
- Skills: `{name}/skill.md` (e.g., `db-migration/skill.md`)
- Rules: `{topic}.md` (e.g., `context.md`)