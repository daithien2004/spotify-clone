# Tokens → Tailwind Utilities (fast lookup)

Nguồn: `frontend/tokens.css` + map `@theme inline` trong `app/globals.css`.

## Semantic alias → class utility

| Token (tokens.css) | Giá trị | Utility class |
|---|---|---|
| `--bg-primary` | `#111212` | `bg-bg-primary` |
| `--bg-secondary` | `#131313` | `bg-bg-secondary` |
| `--bg-tertiary` | `#1e1e1e` | `bg-bg-tertiary` |
| `--bg-elevated` | `#121212` | `bg-bg-elevated` |
| `--bg-deep` | `#101111` | `bg-bg-deep` |
| `--bg-muted` | `#111111` | `bg-bg-muted` |
| `--surface-primary` | `#fefeff` | `bg-surface-primary` |
| `--surface-secondary` | `#feffff` | `bg-surface-secondary` |
| `--surface-foreground` | `#242424` | `text-surface-foreground` |
| `--text-primary` | `#dfdfdf` | `text-text-primary` |
| `--text-secondary` | `#e3e3e3` | `text-text-secondary` |
| `--text-strong` | `#e4e4e4` | `text-text-strong` |
| `--text-soft` | `#c2c2c2` | `text-text-soft` |
| `--text-muted` | `#868686` | `text-text-muted` |
| `--text-hint` | `#8d8d8d` | `text-text-hint` |
| `--text-on-white` | `#242424` | `text-text-on-white` |
| `--text-on-white-sub` | `#3d3d3d` | `text-text-on-white-sub` |
| `--text-on-white-soft` | `#414141` | `text-text-on-white-soft` |
| `--accent-primary` | `#1dd760` | `bg-accent-primary` / `text-accent-primary` |
| `--accent-primary-foreground` | `#073417` | `text-accent-primary-foreground` |
| `--accent-secondary` | `#e2d9ee` | `text-accent-secondary` |
| `--accent-tertiary` | `#edd9eb` | `text-accent-tertiary` |
| `--border-feed` | `#020202` | `border-feed` (qua `border-border-feed`) |

## Radius / spacing đặc biệt

- `rounded-[var(--radius-image-card)]` → `rounded-image-card` (0.5rem) — ảnh bìa.
- `rounded-full` (999px) — nút pill/play/avatar nghệ sĩ.
- `h-[var(--spacing-0-25)]` = 1px — đừng dùng cho slider track (no-op trên Root; track thật ở `ui/slider.tsx`).

## Cách thêm token mới (hex chưa có)

1. Thêm primitive vào `tokens.css` block `:root, .dark` (vd `--smoke-7: #2a2a2a`).
2. Thêm semantic alias (vd `--bg-hover: var(--smoke-7)`).
3. Map sang `@theme inline` trong `globals.css`: `--color-bg-hover: var(--bg-hover);` → có class `bg-bg-hover`.
4. **Không bao giờ** dùng `text-[#hex]` trong component khi token tồn tại — hook chặn.

## Lưu ý

- `text-foreground`/`text-muted-foreground` (shadcn legacy, = `#dfdfdf`/`#8d8d8d`) vẫn còn trong vài component — tương đương `text-text-primary`/`text-text-hint`; migrate dần sang token mới.
- `border-border` = `--border` (trắng 8%). `border-white/5` cũng hợp lệ cho vạch mờ player.