import type { Role } from './enums';

// Mirrors RegisterRequest.java
export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role?: Role;
}

// Mirrors LoginRequest.java
export interface LoginRequest {
  email: string;
  password: string;
}

// Mirrors RefreshRequest.java
export interface RefreshRequest {
  refreshToken: string;
}

// Mirrors AuthResponse.java
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  role: Role;
}
