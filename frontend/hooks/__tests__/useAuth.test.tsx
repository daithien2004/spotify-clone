import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/useAuthStore";
import { useLogin, useUpdateProfile } from "@/hooks/useAuth";
import { AuthService } from "@/services/api/authService";

vi.mock("@/services/api/authService", () => ({
  AuthService: {
    login: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe("useLogin", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({ user: null });
    vi.clearAllMocks();
  });

  it("sets auth when login succeeds (no 2FA)", async () => {
    vi.mocked(AuthService.login).mockResolvedValue({
      userId: "u1", email: "a@b.com", displayName: "A", avatarUrl: null, expiresIn: 900,
    });
    const { result } = renderHook(() => useLogin(), { wrapper });
    act(() => {
      result.current.mutate({ email: "a@b.com", password: "password1" });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user?.displayName).toBe("A");
  });

  it("does NOT set auth when login requires 2FA", async () => {
    vi.mocked(AuthService.login).mockResolvedValue({
      userId: "u1", email: "a@b.com", displayName: "A", avatarUrl: null,
      expiresIn: 0, mfaRequired: true, mfaToken: "mfatok", twoFactorEnabled: true,
    });
    const { result } = renderHook(() => useLogin(), { wrapper });
    act(() => {
      result.current.mutate({ email: "a@b.com", password: "password1" });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user).toBeNull();
    expect(result.current.data?.mfaRequired).toBe(true);
  });
});

describe("useUpdateProfile", () => {
  it("updates store user on success", async () => {
    useAuthStore.setState({ user: { id: "u1", email: "a@b.com", displayName: "Old", avatarUrl: null } });
    vi.mocked(AuthService.updateProfile).mockResolvedValue({
      id: "u1", email: "a@b.com", displayName: "New", avatarUrl: null,
      emailVerified: true, twoFactorEnabled: false,
    });
    const { result } = renderHook(() => useUpdateProfile(), { wrapper });
    act(() => {
      result.current.mutate({ displayName: "New", avatarUrl: null });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user?.displayName).toBe("New");
    expect(useAuthStore.getState().user?.twoFactorEnabled).toBe(false);
  });
});
