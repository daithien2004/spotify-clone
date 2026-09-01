import { api, unwrap, type ApiResponse } from "@/lib/api-client";
import {
  AuthResponse,
  Enroll2faResponse,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
} from "@/types/auth";

/** Hồ sơ trả về từ GET /auth/me — khớp type User của useAuthStore. */
export interface ProfileResponse {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
}

/** Hình thức phản hồi chuẩn của backend (success + payload). */
export interface ApiResult<T> {
  success: boolean;
  data: T;
  message?: string;
}

export class AuthService {
  static async login(request: LoginRequest): Promise<AuthResponse> {
    // api.post trả về envelope {success,data:{payload}} — unwrap để trả payload đúng
    // theo return type AuthResponse (giống convention track/playlist service).
    const envelope = await api.post<ApiResponse<AuthResponse>>("/auth/login", request);
    return unwrap(envelope);
  }

  static async register(request: RegisterRequest): Promise<AuthResponse> {
    const envelope = await api.post<ApiResponse<AuthResponse>>("/auth/register", request);
    return unwrap(envelope);
  }

  static async logout(): Promise<void> {
    return api.post<void>("/auth/logout", {});
  }

  static async refresh(): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/refresh", {});
  }

  static async me(): Promise<ApiResult<ProfileResponse>> {
    return api.get<ApiResult<ProfileResponse>>("/auth/me");
  }

  static async forgotPassword(email: string): Promise<void> {
    await api.post<{ message: string }>("/auth/forgot-password", { email });
  }

  static async resetPassword(token: string, newPassword: string): Promise<void> {
    await api.post<{ message: string }>("/auth/reset-password", { token, newPassword });
  }

  static async sendVerification(email: string): Promise<void> {
    await api.post<{ message: string }>("/auth/send-verification", { email });
  }

  static async verifyEmail(token: string): Promise<void> {
    await api.post<{ message: string }>("/auth/verify-email", { token });
  }

  static async updateProfile(body: UpdateProfileRequest): Promise<ProfileResponse> {
    const envelope = await api.patch<ApiResponse<ProfileResponse>>("/auth/me", body);
    return unwrap(envelope);
  }

  static async enroll2fa(): Promise<Enroll2faResponse> {
    const envelope = await api.post<ApiResponse<Enroll2faResponse>>("/auth/2fa/enroll", {});
    return unwrap(envelope);
  }

  static async verify2faSetup(code: string): Promise<void> {
    await api.post<{ message: string }>("/auth/2fa/verify", { code });
  }

  static async disable2fa(code: string): Promise<void> {
    await api.post<{ message: string }>("/auth/2fa/disable", { code });
  }

  static async verify2faLogin(mfaToken: string, code: string): Promise<AuthResponse> {
    const envelope = await api.post<ApiResponse<AuthResponse>>("/auth/2fa/verify-login", { mfaToken, code });
    return unwrap(envelope);
  }
}
