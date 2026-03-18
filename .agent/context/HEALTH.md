# Project Health & Quality Tracker

This file tracks all active issues related to code quality, security, bugs, and technical debt. It is updated during every development session.

---

## 🛡 Security & Vulnerabilities
| ID | Description | Severity | Status | Service |
|:---|:---|:---|:---|:---|
| SEC-001 | Domain events are currently LOGGED only, not persisted or streamed securely. | Medium | Open | Infrastructure |
| SEC-002 | Redis blacklist for JWT logout not yet implemented. | High | Open | `auth-service` |

---

## 🐞 Bugs & Errors
| ID | Description | Severity | Status | Service |
|:---|:---|:---|:---|:---|
| BUG-001 | Missing `updatedAt` field in some JPA Entities (Audit standard). | Low | Partly Fixed | Multiple |
| BUG-002 | Kafka consumer timeout when reconnecting (Backlog objective). | Medium | Open | Infrastructure |

---

## 🧹 Clean Code & Refactoring (Debt)
| ID | Description | Principle Violated | Status | Service |
|:---|:---|:---|:---|:---|
| CLN-001 | Lack of Integration Tests for cross-layer verification. | Testability | Open | All |
| CLN-002 | Use Case logic might need smaller, focused methods (early returns). | SOLID (SRP) | Open | `track-service` |
| CLN-003 | Hardcoded configurations in some `application.properties`. | Maintainability | Open | Multiple |

---

## 📜 Quality Standards Checklist
- [ ] Every new endpoint has a `security-review`.
- [ ] Every new DB change uses `db-migration` skill.
- [ ] Domain logic is 100% framework-agnostic (Clean Architecture).
- [ ] TDD cycle (Red-Green-Refactor) followed for all logic.
