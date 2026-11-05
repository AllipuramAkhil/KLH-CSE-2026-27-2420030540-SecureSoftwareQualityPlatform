import type { Role } from './enums';

// Mirrors UserResponse.java
export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

// Mirrors UserUpdateRequest.java
export interface UserUpdateRequest {
  fullName?: string;
}
