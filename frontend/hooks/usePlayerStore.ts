import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface Track {
  id: string;
  title: string;
  artist: string;
  imageUrl: string;
  /** duration in seconds (metadata — could differ from real audio length) */
  duration: number;
  /** relative streaming path /api/v1/tracks/{id}/audio */
  audioUrl?: string;
}

interface PlayerState {
  isPlaying: boolean;
  currentTrack: Track | null;
  volume: number;
  /** playback progress 0..100 (% của duration) */
  progress: number;
  /** danh sách phát tiếp theo — index > queueIndex là "sắp phát" */
  queue: Track[];
  queueIndex: number;

  setIsPlaying: (isPlaying: boolean) => void;
  setCurrentTrack: (track: Track | null) => void;
  setVolume: (volume: number) => void;
  setProgress: (progress: number) => void;
  togglePlay: () => void;
  /** Bắt đầu phát từ startIndex trong danh sách (queue = tracks). */
  playQueue: (tracks: Track[], startIndex?: number) => void;
  addToQueue: (track: Track) => void;
  next: () => void;
  previous: () => void;
}

export const usePlayerStore = create<PlayerState>()(
  persist(
    (set) => ({
      isPlaying: false,
      currentTrack: null,
      volume: 0.7,
      progress: 0,
      queue: [],
      queueIndex: -1,

      setIsPlaying: (isPlaying) => set({ isPlaying }),
      setCurrentTrack: (track) => set({ currentTrack: track }),
      setVolume: (volume) => set({ volume }),
      setProgress: (progress) => set({ progress }),
      togglePlay: () => set((state) => ({ isPlaying: !state.isPlaying })),

      playQueue: (tracks, startIndex = 0) =>
        set(() => {
          const track = tracks[startIndex] ?? null;
          return {
            queue: tracks,
            queueIndex: tracks.length > 0 ? startIndex : -1,
            currentTrack: track,
            progress: 0,
            isPlaying: track !== null,
          };
        }),

      addToQueue: (track) =>
        set((state) => ({ queue: [...state.queue, track] })),

      next: () =>
        set((state) => {
          if (state.queue.length === 0 || state.queueIndex < 0) return state;
          const nextIndex = state.queueIndex + 1;
          if (nextIndex >= state.queue.length) {
            // Hết queue → dừng, giữ track cuối trên UI.
            return { isPlaying: false };
          }
          return {
            currentTrack: state.queue[nextIndex],
            queueIndex: nextIndex,
            progress: 0,
            isPlaying: true,
          };
        }),

      previous: () =>
        set((state) => {
          if (state.queue.length === 0 || state.queueIndex < 0) return state;
          if (state.queueIndex === 0) {
            // Đầu queue → quay đầu lại track hiện tại.
            return { progress: 0 };
          }
          const prevIndex = state.queueIndex - 1;
          return {
            currentTrack: state.queue[prevIndex],
            queueIndex: prevIndex,
            progress: 0,
            isPlaying: true,
          };
        }),
    }),
    {
      name: "spotify-player-storage",
      storage: createJSONStorage(() => localStorage),
      // Chỉ persist volume + currentTrack; queue/progress là trạng thái phiên.
      partialize: (state) => ({
        volume: state.volume,
        currentTrack: state.currentTrack,
      }),
    }
  )
);