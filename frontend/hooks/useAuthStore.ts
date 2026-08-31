import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  emailVerified?: boolean;
  twoFactorEnabled?: boolean;
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
        // HttpOnly cookie (server) đã xử lý token — store chỉ giữ user.
        set({ user });
      },

      clearAuth: () => {
        // Server-side logout xóa cookie; client chỉ cần reset state ngay.
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
      }),
    }
  )
);
