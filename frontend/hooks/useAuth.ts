import { useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthService } from "@/services/api/authService";
import { LoginRequest, RegisterRequest, UpdateProfileRequest } from "@/types/auth";
import { useAuthStore } from "./useAuthStore";

export const useLogin = () => {
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (data: LoginRequest) => AuthService.login(data),
    onSuccess: (data) => {
      if (data.mfaRequired) return; // bước 2 nhập TOTP code trong cùng trang (ADR D3)
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
      toast.success("Account created", {
        description: `We sent a verification link to ${data.email}. Check your inbox.`,
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

export const useForgotPassword = () =>
  useMutation({
    mutationFn: (email: string) => AuthService.forgotPassword(email),
  });

export const useResetPassword = () =>
  useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      AuthService.resetPassword(token, newPassword),
  });

export const useVerifyEmail = () =>
  useMutation({
    mutationFn: (token: string) => AuthService.verifyEmail(token),
  });

export const useResendVerification = (email: string) =>
  useMutation({
    mutationFn: () => AuthService.sendVerification(email),
  });

export const useUpdateProfile = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => AuthService.updateProfile(body),
    onSuccess: (data) => {
      setAuth({
        id: data.id,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
        emailVerified: data.emailVerified,
        twoFactorEnabled: data.twoFactorEnabled,
      });
    },
  });
};

export const useEnroll2fa = () =>
  useMutation({ mutationFn: () => AuthService.enroll2fa() });

export const useVerify2faSetup = (onSuccess?: () => void) =>
  useMutation({
    mutationFn: (code: string) => AuthService.verify2faSetup(code),
    onSuccess,
  });

export const useDisable2fa = (onSuccess?: () => void) =>
  useMutation({
    mutationFn: (code: string) => AuthService.disable2fa(code),
    onSuccess,
  });

export const useVerify2faLogin = () => {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: ({ mfaToken, code }: { mfaToken: string; code: string }) =>
      AuthService.verify2faLogin(mfaToken, code),
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

export const useBootstrapAuth = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  return useCallback(async () => {
    try {
      const res = await AuthService.me();
      if (res.success && res.data) {
        setAuth({
          id: res.data.id,
          email: res.data.email,
          displayName: res.data.displayName,
          avatarUrl: res.data.avatarUrl,
          emailVerified: res.data.emailVerified,
          twoFactorEnabled: res.data.twoFactorEnabled,
        });
      } else {
        clearAuth();
      }
    } catch {
      // JWT hết hạn / chưa login — clear state cũ (fix localStorage stale, ADR D7)
      clearAuth();
    }
  }, [setAuth, clearAuth]);
};
