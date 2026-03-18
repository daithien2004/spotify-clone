---
description: Perform a project-wide health scan and update HEALTH.md
---

# /health-check

## Purpose
Exhaustive or targeted scanning of the project to identify security leaks, functional bugs, and architecture violations. Updates `.agent/context/HEALTH.md`.

---

## Trigger
- Manual invocation using `/health-check`.
- Recommended before major releases, after large refactors, or upon USER request.

---

## Phase 1 — Analysis Mode Selection
Choose scan depth:
- **Exhaustive**: Full project scan (Conventions, Security, Code Smells).
- **Targeted**: Scan specific service or layer (e.g., `playlist-service` domain layer).

---

## Phase 2 — Execution

### 1. Architecture Guard (Built-in)
Scan for Clean Architecture violations. Report as `CLN-xxx` in `HEALTH.md`.

#### 🔴 CRITICAL
- **Spring-leak in Domain**: Tìm `org.springframework` trong `domain/`.
- **Persistence-leak in Domain**: Tìm `JpaRepository`, `@Repository`, `@Entity`, `@Table`, `jakarta.persistence` trong `domain/`.
- **Kafka-leak in Domain**: Tìm `org.apache.kafka` trong `domain/`.

#### 🟠 HIGH
- **Business Logic in Controller**: Tìm `@RestController` class có > 3 cấp nested block, hoặc gọi trực tiếp `Repository` thay vì Use Case.
- **God Use Case**: Tìm Use Case inject > 4 dependencies hoặc file > 150 dòng.
- **Direct JPA Entity exposure**: Tìm `JpaEntity` được return từ `application/` hoặc `presentation/` layer.

#### 🟡 MEDIUM
- **Field Injection**: Tìm `@Autowired` không phải constructor injection.
- **Missing Mapper**: Tìm class map thủ công giữa Domain Entity và JPA Entity (không dùng MapStruct).
- **Anemic Domain Model**: Tìm Domain Entity chỉ có getters/setters, không có business method.

### 2. Security Review
Apply `security-review` skill checklist to all relevant service endpoints. Report as `SEC-xxx`.

### 3. Clean Code Sweep
Audit for SOLID/DRY violations based on `CRAFTSMAN.md`. Report as `CLN-xxx`.

### 4. Bug Hunt
Identify functional issues (e.g., missing audit fields, Kafka timeouts). Report as `BUG-xxx`.

---

## Phase 3 — Audit Report & Update
1. **Report Findings**: Present a summary of new issues found and current issues still open.
2. **Update HEALTH.md**: 
   - Add new items with `SEC-`, `BUG-`, or `CLN-` prefixes.
   - Update status of items that are no longer present to `Fixed`.
   - Update `Status` for items currently in progress.

---

## Guidance for AI
- DO NOT run this automatically inside tasks unless requested.
- Prioritize accuracy over speed.
- Cite the exact file and line number for every new issue found.
