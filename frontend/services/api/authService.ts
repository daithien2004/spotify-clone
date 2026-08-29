import { api } from "@/lib/api-client";
import { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

/** Hồ sơ trả về từ GET /auth/me — khớp type User của useAuthStore. */
export interface ProfileResponse {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

/** Hình thức phản hồi chuẩn của backend (success + payload). */
export interface ApiResult<T> {
  success: boolean;
  data: T;
  message?: string;
}

export class AuthService {
  static async login(request: LoginRequest): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/login", request);
  }

  static async register(request: RegisterRequest): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/register", request);
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
}
