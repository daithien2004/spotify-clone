---
name: be-workflow
description: Mandatory working pipeline for ANY backend task in this repo (anything under backend/ or gateway/ — .java files, pom.xml, Flyway migrations, Kafka events, application.properties, docker-compose). Use for every BE task before writing code: boot → plan (discuss + lock skill manifest) → implement with the locked skills and Clean Architecture hard-gates → verify (mvnw test + review) after user acceptance. Applies when generating, fixing, refactoring, or reviewing backend code.
---

# be-workflow — Quy trình làm việc backend

Pipeline 4 bước, bắt buộc cho mọi task chạm `backend/` hoặc `gateway/`. Có **2 gate duyệt**: chốt plan ở bước 2, chốt verify ở bước 4.

## Skill Inventory — skill tồn tại ở đâu (đừng bao giờ đoán "skill ma")

Trước khi kéo/sửa skill nào, **xác nhận skill có thật bằng đường dẫn bên dưới**. Skill nằm trong **plugin cache**, KHÔNG phải `~/.claude/skills/`. Từng có sai lầm kết luận `superpowers:*` là "skill ma" chỉ vì tìm nhầm thư mục — hãy `find ~/.claude/plugins/cache -name SKILL.md`.

| Nguồn | Nơi nằm | Skill/Xuất dùng |
|---|---|---|
| **Plugin superpowers** | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/` | `brainstorming`, `writing-plans`, `test-driven-development` (TDD), `systematic-debugging`, `executing-plans`, `subagent-driven-development`, `verification-before-completion`, `requesting-code-review`, `receiving-code-review` |
| **Plugin feature-dev** | `~/.claude/plugins/cache/claude-plugins-official/feature-dev/<ver>/agents/` | `code-explorer`, `code-architect`, `code-reviewer` (subagents — phóng qua `Agent` tool, không qua `Skill`) |
| **Plugin engineering-advanced-skills** (cộng đồng, đã cài) | `~/.claude/plugins/cache/claude-code-skills/engineering-advanced-skills/2.9.0/skills/` | `migration-architect` (zero-downtime migration, compatibility gate, rollback runbook), `database-designer` (normalization, index optimizer, ERD), `database-schema-designer`, `sql-database-assistant`, `api-design-reviewer` (lint + breaking-change + scorecard ≥ gate), `api-test-suite-builder`, `pr-review-expert`, `ship-gate` (pre-prod 8-category audit), `self-eval` |
| **Repo rules** (KHÔNG phải skill — hiện thân backend standards) | `spotify-clone/.claude/rules/` | `context.md` (tầng Clean Arch, microservices), `conventions.md` §1 (Clean Arch/naming/API/logging), `domain.md` (event map Kafka) |
| **Các skill tự viết cũ `db-migration`/`kafka-event`/`security-review`** | — | Đã **xóa** (2026-08-29): thiếu áp dụng stack/cộng đồng, thay bằng skill cộng đồng ở bước 3. Kafka không có skill riêng — giữ inline qua `domain.md` event map. |

- Task có **logic nghiệp vụ / use case / domain value-object** → bắt buộc `test-driven-development` (red→green→refactor, viết test trước).
- Task chạm **schema/DB/migration** → `migration-architect` + `database-designer` (theo bước 3.5).
- Task chạm **endpoint/API contract** → `api-design-reviewer` (theo bước 3.7) + `api-test-suite-builder` khi có test API.
- Task lớn / đổi cấu trúc tầng / thêm module → `code-architect` + `writing-plans` + cân nhắc mục **Design Note** (2–3 dòng ghi quyết định cấu trúc — tương tự ADR) trong plan.
- Task cần **review độc lập** sau khi code xong → `pr-review-expert` (skill) hoặc `code-reviewer` (subagent) + `requesting-code-review` để chốt approve.

## Bước 1 · Khởi động

1. Đọc `.claude/rules/` (context, conventions, domain, craftsman) + `CLAUDE.md` + `PROJECT_STATUS.md`.
2. Xem `git status` để biết context làm việc.
3. **Nhận diện task BE** bằng tín hiệu:

| Loại tín hiệu | Ví dụ |
|---|---|
| Từ khoá backend | entity, usecase, controller, endpoint, migration, Kafka event, repository, domain service, application service, port, adapter |
| Tên công nghệ BE | Java, Spring Boot, JPA/Hibernate, Flyway, Spring Cloud Gateway, Java 21, Maven, `.java`, `pom.xml`, `application.properties` |
| Đường dẫn file | task nhắc tới `backend/**` hoặc `gateway/**` |
| Bối cảnh workspace | đang làm trong backend / ảnh hưởng file BE |

4. Xử lý ranh giới:
   - Task **hỗn hợp** (FE + backend): be-workflow chỉ bao phần BE; phần FE đi theo `fe-workflow`, nêu rõ ranh giới trong plan.
   - Task **mơ hồ** (không tín hiệu nào): hỏi 1 câu xác nhận — "task này có phần chạm backend không?".
5. Xác nhận là task BE → tuyên bố: *"Đang chạy be-workflow"*.

## Bước 2 · PLAN — trao đổi & chốt skill

**Tự động** kéo skill nhóm plan theo độ lớn task (thuộc inventory mục trên — superpowers/feature-dev từ plugin cache, xác nhận bằng `find` trước khi dùng; `feature-dev:*` là subagent qua `Agent` tool):

| Cỡ task | Skill plan được tự dùng |
|---|---|
| Mọi task | `superpowers:brainstorming` (trao đổi, hỏi làm rõ, đề xuất hướng) |
| Cần khảo sát pattern | `feature-dev:code-explorer` (subagent đọc code/kiến trúc có sẵn) |
| Task lớn / đổi cấu trúc | `feature-dev:code-architect` + `superpowers:writing-plans` (plan file, kèm Design Note) |

Trình tự:
1. **Khảo sát code** — `codebase-memory` MCP trước (search_graph/trace_path/get_code_snippet) → Grep/Glob/Read cho chi tiết. Luôn đọc **Local Context** trước khi chạm module: xác định module (auth / playlist / common), đọc file thuộc module đó để bắt pattern tầng (domain/application/infrastructure/presentation) → **tái sử dụng, không viết mới**.
2. **Trao đổi với người dùng** — hỏi 1 câu/lần (ưu tiên multiple-choice): yêu cầu, chạm module/service nào, có đổi schema (`Flyway`) / event (`Kafka`) / endpoint (`security`) không, success criteria.
3. **Đề xuất cách tiếp cận** + danh sách files chạm (theo tầng).
4. **Chốt skill manifest** — liệt kê ĐẦY ĐỦ các skill sẽ dùng ở bước 3 (ví dụ: `test-driven-development` nếu có logic usecase/domain, `code-reviewer` nếu có review…).
5. 🛑 **Gate duyệt** — trình plan + manifest, **chờ "yes"** từ người dùng. Tuyệt đối không tự nhảy sang bước 3.

## Bước 3 · Thực thi (dùng ĐÚNG skill đã chốt + hard-gate Clean Architecture)

1. Áp các skill **trong manifest đã chốt ở bước 2** (đều phải thuộc inventory mục trên).
2. **Hard-gate Clean Architecture** (`conventions.md` §1 — không thương lượng):
   - **Dependency rule:** dependencies chỉ chảy vào trong: `presentation → application → domain`; `infrastructure → domain`. Cấm import ngược.
   - **Domain purity:** `domain/` KHÔNG BAO GIỜ import Spring/JPA/external framework (kiểm tra imports).
   - **Constructor injection** — cấm `@Autowired` field injection.
   - **Method scope** — early returns, method < 40 dòng, tránh nested sâu.
   - **Naming** — package lowercase, class/interface PascalCase, method/var camelCase, constant CONSTANT_CASE, test `should_X_when_Y`.
   - **Không business logic trong `@Controller`** — controller chỉ gọi use case.
3. **Logic nghiệp vụ → TDD** — `test-driven-development` (test trước, red→green→refactor). Test đặt theo module (`src/test/java/...`), naming `should_X_when_Y`.
4. **API contracts** (khi chạm endpoint/DTO — `conventions.md` §2):
   - `ApiResponse<T>` + `@ControllerAdvice` global exception mapping.
   - `@Valid` + Bean Validation; DTO dạng `record`.
   - Security: BCrypt password, CORS explicit (không wildcard prod).
5. **DB migration** (khi đổi schema): gọi skill `migration-architect` (compatibility gate + rollback runbook cho thay đổi lớn) + `database-designer` (schema/index design). Luôn **qua Flyway** — tạo file migration mới trong thư mục migrations, tuyệt đối không sửa schema tay / không auto-DDL.
6. **Kafka event** (khi giao tiếp cross-service): domain event đặt trong `domain/event/`, producer ở infrastructure; tuân **event map** trong `domain.md` (Subject/Action/From/To — ví dụ `User.Registered` auth→user). Không có skill cộng đồng riêng cho Kafka trên máy — theo inline + `domain.md`.
7. **API/security** (mọi endpoint mới): gọi skill `api-design-reviewer` (lint + breaking-change + scorecard ≥ gate) khi thêm/sửa endpoint. Checklist security inline: auth/authz trên resource, validate input, không log secret/password, rate-limit nếu public, không lộ internal error cho client (`GlobalExceptionHandler`).
8. **Logging** — SLF4J, đúng cấp (ERROR/WARN/INFO/DEBUG); **API docs** — annotate endpoint với Springdoc OpenAPI.
9. **Comment discipline — Comments = WHY, không phải WHAT.** Chỉ comment thứ KHÔNG đọc được từ code: rationale thiết kế, quirk framework/library, contract Kafka/Flyway, caveat (transaction, security). Cấm comment lặp tên method/biến hay mô tả markup hiển nhiên. Comment stale = bug: đổi hành vi → cập nhật/xóa comment cũ. Trong Java: ưu tiên self-documenting; comment ≤ 3 dòng.
10. Hoàn tất → ✋ **HỎI người dùng**: *"Bạn chấp nhận để mình chạy verify chưa?"* — chờ gật đầu trước khi sang bước 4.

## Bước 4 · Verify (chỉ chạy sau khi được người dùng đồng ý)

1. Chạy test thật của module bị chạm:
   - Chạm `backend/` → `cd backend && ./mvnw test` (multi-module: chạy cả common-lib + auth-service + playlist-service)
   - Chạm `gateway/` → `cd gateway && ./mvnw test`
   - Chạm đúng 1 service → `./mvnw -pl <module> test`
   - ⚠️ **Windows + path `THIEN'PC`:** bash script `./mvnw` có thể fail `ClassNotFoundException` vì bash mất ký tự `'` trong `$HOME`. Workaround đã verify (2026-08-29): chạy Maven dist trực tiếp qua cmd: `cmd.exe /c "C:\Users\THIEN'PC\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd test"` (hoặc dùng `mvnw.cmd` từ PowerShell).
   - (Backend hiện chưa có JaCoCo/Checkstyle/SpotBugs trong pom — **không tự thêm** vì trái `context.md` "Không tự thêm dependency ngoài stack đã chốt"; thêm tooling phải được người dùng duyệt riêng.)
2. **Clean Architecture self-check** — rà imports của file mới/sửa: không có Spring/JPA trong `domain/`, không @Autowired field, controller không chứa business logic.
3. **Review độc lập** (nếu manifest chốt): skill `pr-review-expert` (rà PR/changes theo checklist cộng đồng) hoặc phóng `feature-dev:code-reviewer` (subagent) rà changed files → vá finding thật trước khi báo xong.
4. Trình **bằng chứng thật** (output của `mvnw test` — BUILD SUCCESS + số test) — không báo "xong" khi chưa xanh.
5. Nếu người dùng chưa hài lòng → quay lại bước 3 sửa, lặp lại verify. Không tự lan rộng scope ngoài những gì đã chốt ở bước 2.