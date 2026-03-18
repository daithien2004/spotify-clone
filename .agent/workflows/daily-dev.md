---
description: Daily Dev Workflow - Context-aware execution of specific developer requests
---

# Daily Dev Workflow

## Purpose
Ensure every developer request is executed with full project context, adhering to established conventions and leveraging available skills.

---

## Trigger
Use this workflow for any technical request during a dev session.

**Invocation:** "Let's start today's session", "daily dev", or any specific task request.

---

## Phase 1 — Context & Skill Alignment

AI sẽ:
1. **Quick Context Sync**: Đọc qua các file `.agent/context/` (trừ `HEALTH.md`) để nắm bắt status.
2. **Identify Skills**: Xác định skill cần dùng (ví dụ: `db-migration` nếu có đổi DB).

---

## Phase 2 — Execution Plan

Produce a brief execution plan and ask for confirmation:

1.  **Analyze**: Breakdown the request based on the gathered context.
2.  **Propose**: List the files to be modified/created and the skills to be used.
3.  **Verify**: Define how the change will be tested (TDD approach).

---

## Phase 3 — Context-Driven Implementation

1.  **Strict Adherence**: Follow `CONVENTIONS.md` (Clean Architecture, Naming, etc.) and `CRAFTSMAN.md` (Quality, TDD) strictly.
2.  **Skill Application**: If a skill was identified, use its specific patterns and scripts.
3.  **Self-Correction**: If the plan evolves during implementation, update the user with the rationale.

---

## Phase 4 — Verification & Update

1.  **Test**: Execute verification steps as planned.
2.  **Update PROGRESS.md**: Nếu thay đổi ảnh hưởng đến project milestones.
