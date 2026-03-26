import { useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { AuthService } from "@/services/api/authService";
import { LoginRequest, RegisterRequest } from "@/types/auth";
import { useAuthStore } from "./useAuthStore";

export const useLogin = () => {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (data: LoginRequest) => AuthService.login(data),
    onSuccess: (data) => {
      setAuth({
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
      });
      router.push("/");
    },
  });
};

export const useRegister = () => {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (data: RegisterRequest) => AuthService.register(data),
    onSuccess: (data) => {
      setAuth({
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
      });
      router.push("/");
    },
  });
};

export const useLogout = () => {
  const router = useRouter();
  const clearAuth = useAuthStore((state) => state.clearAuth);

  return useCallback(async () => {
    try {
      await AuthService.logout();
    } finally {
      clearAuth();
      router.push("/login");
    }
  }, [clearAuth, router]);
};

export const useCurrentUser = () => {
  return useAuthStore((state) => state.user);
};

export const useIsAuthenticated = () => {
  return useAuthStore((state) => state.isAuthenticated());
};
