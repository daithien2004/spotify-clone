---
name: frontend-conventions
description: Mandatory standards for all frontend work in this repo (Next.js App Router, Tailwind v4, shadcn/ui, Zustand). Use for ANY task editing .tsx/.ts/.css files under frontend/ — token-only styling, shadcn/cva extension rules, responsive/container-query, Zustand granular selectors, Server vs Client, and the Figma→code workflow. Applies when generating, fixing, or refactoring UI, or comparing against design.
metadata:
  type: project
---

# Spotify Clone — Frontend Conventions

Mọi code frontend trong repo này **bắt buộc** tuân theo các quy tắc dưới đây. Đọc các file `references/` khi cần chi tiết.

## 1. Styling — token-only (hard gate)

- Chỉ dùng các utility sinh từ design token: `bg-*`, `text-*`, `border-*`, `ring-*`, `accent-*` map từ `frontend/tokens.css` qua `@theme inline` trong `frontend/app/globals.css`.
- **CẤM** class arbitrary-hex `text-[#XXXX]`, `bg-[#XXXX]`, `border-[#XXXX]`… khi hex đó đã có token tương đương. Hook `block-hardcoded-hex.sh` chặn cứng các trường hợp trùng giá trị.
- Hex mới từ Figma mà chưa có token → **thêm token mới** vào `frontend/tokens.css` (`--bg-*`, `--text-*`, …) rồi mới dùng — không hardcode trong component.
- Bảng tra nhanh hex→token/class: `references/tokens.md`.

## 2. Components — shadcn/ui + cva

- Component nào shadcn/ui đã cung cấp → dùng `npx shadcn@latest add <name>`, **không viết lại tay** (mất a11y của Radix, lệch version).
- Component UI nằm ở `frontend/components/ui/` (button, card, slider, scroll-area…). Component feature nằm ở `frontend/components/` + `player/`.
- Muốn thêm variant/kiểu mới → **mở rộng cva** trong `frontend/components/ui/button.tsx` (`buttonVariants.variants`), không đặt class random trong JSX.
- Khi không chắc dùng component có sẵn vs custom → ưu tiên component có sẵn; custom chỉ khi cần khác biệt rõ về hành vi.

## 3. Responsive & layout

- Top-level (MainLayout): breakpoint viewport `lg:`/`xl:` gate sidebar; hệ drag-resize dùng CSS var `--left-w`/`--right-w` + `localStorage` (xem `references/layout.md`).
- Vùng nội dung có độ rộng thay đổi theo sidebar (**feed, grid card**) → **container query**: thêm `@container`, dùng `@lg:`/`@2xl:` (đã áp dụng ở `HomeFeed.tsx`). Không dùng viewport breakpoint cho những vùng này.
- Grid card chuẩn: `grid grid-cols-2 @lg:grid-cols-3 @2xl:grid-cols-4 gap-4`.
- Tiền lệ `@lg`/`@2xl` (container) đo theo **container**, KHÔNG phải viewport.

## 4. Zustand — granular selector (hard gate)

- **CẤM** destructure cả store: `const { a, b } = usePlayerStore()`.
- Luôn dùng selector granular từng field: `usePlayerStore((s) => s.isPlaying)`. Pattern chuẩn: `frontend/components/MusicCard.tsx:33` và `frontend/components/Player.tsx`.
- Action/field đặt riêng, `partialize` chỉ giữ field cần persist (xem `references/state.md`).

## 5. Server vs Client Component

- Mặc định **Server Component** (không `'use client'`). Thêm `'use client'` chỉ khi cần: state (`useState`), event handler, hook React, `next/navigation`, React Query, Zustand.
- Data fetching ưu tiên Server Component `async/await` hoặc React Query (prefetch + `HydrationBoundary`). Không dùng `useEffect` để fetch.

## 6. Figma → code workflow

1. Lấy dữ liệu từ Figma MCP: `mcp__figma-community__get_figma_data` (layout, copy, hex chính xác — dùng file `figma`/node id có sẵn).
2. Đối chiếu hex với `frontend/tokens.css`: trùng → dùng token; chưa có → thêm token + map `@theme inline` (globals.css), không hardcode.
3. Convert Auto Layout Figma → flex/grid: `flex` + `gap` (thay margin lẻ), `items-`/`justify-` theo alignment trong Figma.
4. Kiểm chứng visual cuối bằng `npm run dev` + screenshot, so sánh từng vùng với Figma.

## 7. Performance & quality (tóm tắt)

- `next/image` cho mọi ảnh (đúng `sizes`, `fill` khi cần layout), `next/font` cho font.
- Memo component nặng (`memo`), handler bọc `useCallback`, `useSyncExternalStore` thay `useState`+mount-pattern khi cần SSR-consistent.
- Lint/build phải xanh trước khi báo xong (`npm run lint`, `npm run build`).