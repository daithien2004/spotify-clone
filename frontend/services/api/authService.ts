import { apiClient } from "@/lib/api-client";

import { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

export class AuthService {
  static async login(request: LoginRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>("/auth/login", request);
  }

  static async register(request: RegisterRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>("/auth/register", request);
  }
}
