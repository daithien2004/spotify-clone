import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface Track {
  id: string;
  title: string;
  artist: string;
  imageUrl: string;
  duration: number;
}

interface PlayerState {
  isPlaying: boolean;
  currentTrack: Track | null;
  volume: number;
  progress: number;
  
  // Actions
  setIsPlaying: (isPlaying: boolean) => void;
  setCurrentTrack: (track: Track | null) => void;
  setVolume: (volume: number) => void;
  setProgress: (progress: number) => void;
  togglePlay: () => void;
}

export const usePlayerStore = create<PlayerState>()(
  persist(
    (set) => ({
      isPlaying: false,
      currentTrack: null,
      volume: 0.7,
      progress: 0,

      setIsPlaying: (isPlaying) => set({ isPlaying }),
      setCurrentTrack: (track) => set({ currentTrack: track }),
      setVolume: (volume) => set({ volume }),
      setProgress: (progress) => set({ progress }),
      togglePlay: () => set((state) => ({ isPlaying: !state.isPlaying })),
    }),
    {
      name: "spotify-player-storage",
      storage: createJSONStorage(() => localStorage),
      // We only want to persist certain fields
      partialize: (state) => ({ 
        volume: state.volume, 
        currentTrack: state.currentTrack 
      }),
    }
  )
);
