export interface AuthResponse {
  token: string;
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
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
