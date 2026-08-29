# Zustand Store — chuẩn tổ chức

File mẫu: `frontend/hooks/usePlayerStore.ts` (player state) và `frontend/hooks/useAuthStore.ts` (auth).

## Bắt buộc

1. **Granular selector khi đọc**:
   ```ts
   // ✅ đúng
   const isPlaying = usePlayerStore((s) => s.isPlaying);
   const setCurrentTrack = usePlayerStore((s) => s.setCurrentTrack);
   // ❌ sai — re-render mọi dispatch
   const { isPlaying, currentTrack, setVolume } = usePlayerStore();
   ```

2. **Field/action tách riêng**: state (data) + action (hàm đổi state) trong cùng interface, action đặt cuối.

3. **`partialize` chỉ giữ field cần persist** (tránh lưu thừa, kẹt storage):
   ```ts
   partialize: (state) => ({ volume: state.volume, currentTrack: state.currentTrack }),
   ```

4. **Unit nhất quán**: ghi rõ quy ước trong interface. Player: `progress` 0–100, `volume` 0–1 (chuyển `*100`/`/100` tại slider).

## Pattern player (Spotify-like)

```ts
interface PlayerState {
  isPlaying: boolean;
  currentTrack: Track | null;
  volume: number;   // 0–1
  progress: number; // 0–100
  setIsPlaying: (v: boolean) => void;
  setCurrentTrack: (t: Track | null) => void;
  setVolume: (v: number) => void;
  setProgress: (v: number) => void;
  togglePlay: () => void;
}
```

- `setProgress` đã có — slider progress phải `onProgressChange={setProgress}` (Player.tsx).

## Auth (token rỗng trên client)

- `useAuthStore` chỉ giữ `user`; token nằm HttpOnly cookie (server) → `partialize` KHÔNG lưu accessToken.
- `isAuthenticated()` qua `get().user !== null`.

## Hydration/SSR

- Client component dùng `useSyncExternalStore` (pattern TopNav `useIsMounted`) thay `useState`+mounted để tránh hydration mismatch.
- Đừng đọc store ở render Server Component (localStorage không tồn tại).