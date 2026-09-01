import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("@/lib/api-client", () => ({
  api: { get: vi.fn(), post: vi.fn(), patch: vi.fn() },
  unwrap: (envelope: { data: unknown }) => envelope.data,
}));

import { api } from "@/lib/api-client";
import { AuthService } from "@/services/api/authService";

describe("AuthService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("forgotPassword posts email to /auth/forgot-password", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.forgotPassword("a@b.com");
    expect(api.post).toHaveBeenCalledWith("/auth/forgot-password", { email: "a@b.com" });
  });

  it("resetPassword posts token+newPassword", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.resetPassword("tok", "NewPass123");
    expect(api.post).toHaveBeenCalledWith("/auth/reset-password", {
      token: "tok",
      newPassword: "NewPass123",
    });
  });

  it("verifyEmail posts token", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.verifyEmail("vtok");
    expect(api.post).toHaveBeenCalledWith("/auth/verify-email", { token: "vtok" });
  });

  it("updateProfile patches /auth/me and returns unwrapped profile", async () => {
    // API trả envelope {success,data:{payload}} — AuthService phải unwrap(data) trước khi trả
    vi.mocked(api.patch).mockResolvedValue({
      success: true,
      data: {
        id: "1", email: "a@b.com", displayName: "New", avatarUrl: null,
        emailVerified: true, twoFactorEnabled: false,
      },
    });
    const r = await AuthService.updateProfile({ displayName: "New", avatarUrl: null });
    expect(api.patch).toHaveBeenCalledWith("/auth/me", { displayName: "New", avatarUrl: null });
    expect(r.displayName).toBe("New");
    expect(r.emailVerified).toBe(true);
  });

  it("enroll2fa POSTs /auth/2fa/enroll and returns unwrapped QR data", async () => {
    vi.mocked(api.post).mockResolvedValue({
      success: true,
      data: { otpauthUrl: "otpauth://...", qrDataUri: "data:image/png;base64,x" },
    });
    const r = await AuthService.enroll2fa();
    expect(api.post).toHaveBeenCalledWith("/auth/2fa/enroll", {});
    expect(r.qrDataUri).toBe("data:image/png;base64,x");
  });

  it("verify2faLogin POSTs mfaToken+code", async () => {
    vi.mocked(api.post).mockResolvedValue({ userId: "1", email: "a@b.com", displayName: "A", avatarUrl: null, expiresIn: 900 });
    await AuthService.verify2faLogin("mfatok", "123456");
    expect(api.post).toHaveBeenCalledWith("/auth/2fa/verify-login", { mfaToken: "mfatok", code: "123456" });
  });
});
