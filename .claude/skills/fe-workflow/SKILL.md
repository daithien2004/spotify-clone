---
name: fe-workflow
description: Mandatory working pipeline for ANY frontend task in this repo (anything under frontend/ — .tsx/.ts/.css, app/, components/, hooks/, services/, lib/). Use for every FE task before writing code: boot → plan (discuss + lock skill manifest) → implement with the locked skills → verify after user acceptance. Applies when generating, fixing, refactoring, or reviewing frontend code.
---

# fe-workflow — Quy trình làm việc frontend

Pipeline 4 bước, bắt buộc cho mọi task chạm `frontend/`. Có **2 gate duyệt**: chốt plan ở bước 2, chốt verify ở bước 4.

## Skill Inventory — skill tồn tại ở đâu (đừng bao giờ đoán "skill ma")

Trước khi kéo/sửa skill nào, **xác nhận skill có thật bằng đường dẫn bên dưới**. Skill nằm trong **plugin cache**, KHÔNG phải `~/.claude/skills/`. Từng có sai lầm kết luận `superpowers:*` là "skill ma" chỉ vì tìm nhầm thư mục — hãy `find ~/.claude/plugins/cache -name SKILL.md`.

| Nguồn | Nơi nằm | Skill điển hình |
|---|---|---|
| **Plugin superpowers** | `~/.claude/plugins/cache/claude-plugins-official/superpowers/<ver>/skills/` | `brainstorming`, `writing-plans`, `test-driven-development` (TDD), `systematic-debugging`, `executing-plans`, `subagent-driven-development`, `verification-before-completion`, `frontend-design`, `dispatching-parallel-agents` |
| **Plugin feature-dev** | `~/.claude/plugins/cache/claude-plugins-official/feature-dev/<ver>/agents/` | `code-explorer`, `code-architect`, `code-reviewer` (subagents: explore/design/code-review) |
| **Repo** `.claude/skills/` | `spotify-clone/.claude/skills/<name>/SKILL.md` | `frontend-conventions` (bắt buộc cho task FE), `shadcn`, `fe-workflow`, `migrate-radix-to-base`, `vercel-react-best-practices` (skill thật từ `vercel-labs/agent-skills` — xem `skills-lock.json`) |
| **Plugin claude-code-skills** | `~/.claude/plugins/cache/claude-code-skills/<name>/<ver>/skills/` | `a11y-audit` (WCAG 2.2 AA: contrast, semantics, focus) |

- Khi task dựng **UI mới từ Figma**: thêm `frontend-design` (thẩm mỹ/typography), `shadcn` (component chuẩn repo), `vercel-react-best-practices` (perf), `a11y-audit` (độ tương phản/accessibility).
- Khi task có **logic thuần tuý** (filter, map, transform): bắt buộc `test-driven-development` (red→green→refactor) cho phần logic đó.

## Bước 1 · Khởi động

1. Đọc `.claude/rules/` (context, conventions, craftsman, domain) + `CLAUDE.md`.
2. Xem `git status` để biết context làm việc.
3. **Nhận diện task FE** bằng tín hiệu:

| Loại tín hiệu | Ví dụ |
|---|---|
| Từ khoá UI/giao diện | màn hình, trang, component, layout, style, màu, animation, responsive, hover, Figma |
| Tên công nghệ FE | Next.js, React, Tailwind, shadcn, Zustand, React Query, `.tsx`, `.css` |
| Đường dẫn file | task nhắc tới `frontend/**` |
| Bối cảnh workspace | đang làm trong frontend / ảnh hưởng file FE |

4. Xử lý ranh giới:
   - Task **hỗn hợp** (backend + FE): fe-workflow chỉ bao phần FE; phần backend đi theo rule backend, nêu rõ ranh giới trong plan.
   - Task **mơ hồ** (không có tín hiệu nào): hỏi 1 câu xác nhận — "task này có phần chạm frontend không?".
5. Xác nhận là task FE → tuyên bố: *"Đang chạy fe-workflow"*.

## Bước 2 · PLAN — trao đổi & chốt skill

**Tự động** kéo skill nhóm plan theo độ lớn task — không cần bạn chỉ định (các skill này thuộc inventory mục trên — superpowers/feature-dev từ plugin cache, xác nhận bằng `find` trước khi dùng):

| Cỡ task | Skill plan được tự dùng |
|---|---|
| Mọi task | `superpowers:brainstorming` (trao đổi, hỏi làm rõ, đề xuất hướng) |
| Cần khảo sát pattern | `feature-dev:code-explorer` (subagent đọc code/phân tích có sẵn) |
| Task lớn / đổi cấu trúc | `feature-dev:code-architect` + `superpowers:writing-plans` (plan file) |

LƯU Ý: 2 skill `superpowers:brainstorming` và `test-driven-development` cũng có thể gọi qua `Skill` tool. `feature-dev:*` là **subagent** (phóng qua `Agent` tool), không phải skill gọi bằng `Skill` — đọc trước `agents/*.md` để biết model/tools.

Trình tự:
1. **Khảo sát code** — `codebase-memory` MCP trước (search_graph/trace_path/get_code_snippet) → Grep/Glob/Read cho chi tiết. Luôn đọc trước `frontend/tokens.css` + `frontend-conventions/references/*` để biết token/pattern có sẵn → **tái sử dụng, không viết mới**.
2. **Trao đổi với người dùng** — hỏi 1 câu/lần (ưu tiên multiple-choice): yêu cầu, đối chiếu Figma?, ràng buộc, success criteria.
3. **Đề xuất cách tiếp cận** + danh sách files chạm.
4. **Chốt skill manifest** — liệt kê ĐẦY ĐỦ các skill sẽ dùng ở bước 3 (ví dụ: `frontend-conventions` + `test-driven-development` nếu có logic, `shadcn` nếu dùng component…).
5. 🛑 **Gate duyệt** — trình plan + manifest, **chờ "yes"** từ người dùng. Tuyệt đối không tự nhảy sang bước 3.

## Bước 3 · Thực thi (dùng ĐÚNG skill đã chốt)

1. Áp các skill **trong manifest đã chốt ở bước 2** (đều phải thuộc inventory mục trên) + hard-gate `frontend-conventions`:
   - **Token-only styling** — cấm arbitrary-hex (`text-[#…]`, `bg-[#…]`) khi token tương đương đã có.
   - **Granular selector** Zustand — cấm destructure cả store (`const {a,b} = useX()`).
   - **Server/Client đúng default** — mặc định Server Component, thêm `"use client"` chỉ khi cần.
   - **Container query** cho vùng feed/grid (`@container` + `@lg:`/`@2xl:`).
   - Component shadcn qua **cva** khi cần variant — không viết lại component.
2. Logic phức tạp → **TDD** (red-green-refactor) — skill `test-driven-development` từ superpowers.
3. UI mới từ Figma → áp thêm theo manifest: `frontend-design` (thẩm mỹ), `shadcn` (component chuẩn repo), `vercel-react-best-practices` (perf), `a11y-audit` (contrast/semantics/focus).
4. **Comment discipline — Comments = WHY, không phải WHAT.** Chỉ comment thứ KHÔNG đọc được từ code; **cấm** comment lặp code / mô tả markup hiển nhiên / liệt kê cấu trúc component. Ví dụ:
   - ✅ Giữ: rationale thiết kế (tại sao làm vậy), quirk thư viện (Radix aria…), ràng buộc contract/backend, caveat (SSR, localStorage, HttpOnly cookie), nguồn tham chiếu (Figma node, nguồn demo data), ghi chú hex trong `tokens.css`/`globals.css`.
   - ❌ Xóa: dòng lặp lại tên biến/hàm, marker “section” lặp markup (`{/* Card nav */}`), doc-block ≥4 dòng liệt kê cấu trúc hiển nhiên (nén còn ≤3 dòng — 1 dòng Figma node + 1–2 dòng mục đích), comment lặp rule đã ghi trong `conventions.md`.
   - Comment stale = bug: đổi hành vi → phải cập nhật/xóa comment cũ.
   - **KHÔNG sửa comment trong component shadcn đã vendor** (`components/ui/*` do CLI sinh) — giữ nguyên để diff được với upstream.
   - Thống nhất 2-space / double-quote.
5. Hoàn tất → ✋ **HỎI người dùng**: *"Bạn chấp nhận để mình chạy verify chưa?"* — chờ gật đầu trước khi sang bước 4.

## Bước 4 · Verify (chỉ chạy sau khi được người dùng đồng ý)

1. Chạy tuần tự: `npx tsc --noEmit` → `npm run lint` → test ảnh hưởng (`vitest`) → `npm run build` nếu thay đổi structural/layout.
2. Trình **bằng chứng thật** (output của các lệnh) — không báo "xong" khi chưa xanh.
3. Nếu người dùng chưa hài lòng → quay lại bước 3 sửa, lặp lại verify. Không tự lan rộng scope ngoài những gì đã chốt ở bước 2.