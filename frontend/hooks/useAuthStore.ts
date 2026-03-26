import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

interface AuthState {
  user: User | null;
  
  setAuth: (user: User) => void;
  clearAuth: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,

      setAuth: (user) => {
        // Token is now handled by HttpOnly cookie from server
        set({ user });
      },

      clearAuth: () => {
        // Cookie clearing is handled by server-side logout, 
        // but we can also clear client-side user state instantly.
        set({ user: null });
      },

      isAuthenticated: () => {
        return get().user !== null;
      },
    }),
    {
      name: "spotify-auth-storage",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        // No accessToken persisted in localStorage anymore!
      }),
    }
  )
);
