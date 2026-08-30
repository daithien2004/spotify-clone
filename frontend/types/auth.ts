export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  userId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  expiresIn: number;
  mfaRequired?: boolean;
  mfaToken?: string;
  twoFactorEnabled?: boolean;
  emailVerified?: boolean;
}

export interface UpdateProfileRequest {
  displayName?: string;
  avatarUrl?: string | null;
}

export interface Enroll2faResponse {
  otpauthUrl: string;
  qrDataUri: string;
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
