import { api } from "@/lib/api-client";
import { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

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
}
