"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { User } from "@/types";
import { authApi } from "@/lib/api/auth";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from "@/lib/constants";
import { clearTokens } from "@/lib/api/client";
import { mapAuthResponseToUser } from "@/lib/api/mappers";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  /** identifier = email for SUPER_ADMIN/ADMIN, or Student ID (e.g. RRT2026S0001) for STUDENT. */
  login: (identifier: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
  setHydrated: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      isHydrated: false,

      login: async (identifier, password) => {
        const res = await authApi.login({ identifier, password });
        const user = mapAuthResponseToUser(res);
        localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken);
        localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
        document.cookie = `rr_role=${res.role}; path=/; max-age=604800; SameSite=Lax`;
        set({ user, isAuthenticated: true });
        return user;
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch {
          // ignore network errors on logout — tokens are cleared client-side regardless
        }
        clearTokens();
        document.cookie = "rr_role=; path=/; max-age=0";
        set({ user: null, isAuthenticated: false });
      },

      setUser: (user) => set({ user, isAuthenticated: !!user }),
      setHydrated: () => set({ isHydrated: true }),
    }),
    {
      name: "rr-lms-auth",
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated }),
      onRehydrateStorage: () => (state) => state?.setHydrated(),
    }
  )
);
