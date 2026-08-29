# Layout & Responsive Conventions

## MainLayout — hệ drag-resize sidebar

- File: `frontend/components/MainLayout.tsx`.
- 2 sidebar dùng CSS var: wrapper trái `width: var(--left-w)`, wrapper phải `width: var(--right-w)`.
- Drag tay cầm (`w-1.5 cursor-col-resize`): giữ-pointer mousemove trên `document`, `e.preventDefault()` chống select, clamp:
  - `--left-w`: 220 → 480px
  - `--right-w`: 260 → 440px
- **Persist**: lưu `{left, right}` vào `localStorage["spotify-layout-widths"]` khi thả chuột (`persistWidths`), hydrate khi mount (useEffect). Không dùng state cho width — chỉ ref + CSS var (tránh re-render).
- Responsive gate: sidebar trái `hidden lg:block`, sidebar phải + handle phải `hidden xl:block`. `rightWidth` vô hiệu dưới xl.
- **Cleanup**: khi unmount giữa lúc kéo → dọn listener qua `cleanupRef` (chống leak).

## Container query — cho vùng phụ thuộc độ rộng

- **Khi nào**: nội dung nằm cạnh sidebar kéo được (feed, grid), độ rộng thay đổi theo drag → **container query**, không dùng viewport breakpoint.
- Đánh dấu container: class `@container` trên phần tử cha (vd `ScrollArea` trong `HomeFeed.tsx`).
- Grid chuẩn: `grid grid-cols-2 @lg:grid-cols-3 @2xl:grid-cols-4 gap-4`.
  - `@lg` = container ≥ 32rem → 3 cột; `@2xl` = container ≥ 42rem → 4 cột.
  - **Khác viewport**: `lg:`/`2xl:` đo theo cửa sổ — cấm dùng cho grid nội dung này.
- Kéo sidebar 220→480 làm feed ±260px → số cột card tự đổi.

## Top-level (không nghiêng): viewport breakpoint

- Sidebar hiện/ẩn, preview bar (`hidden sm:inline-flex`), TopNav → dùng `sm/lg/xl` viewport bình thường.

## Height chain

- `h-screen` root (MainLayout) → `TopNav h-[76px]` → `main flex-1` → `PreviewBar h-[87px]` → `Player h-[88px]`.
- Panel nội dung cuộn qua `ScrollArea` (`h-full`), không scroll cả trang.