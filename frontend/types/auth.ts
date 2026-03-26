export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  userId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  avatarUrl?: string;
}
