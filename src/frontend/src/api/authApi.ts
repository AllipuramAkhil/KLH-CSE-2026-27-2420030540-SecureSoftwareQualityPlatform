import { apiClient } from './axios';
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth';
import type { UserResponse } from '@/types/user';

export const authApi = {
  register: (payload: RegisterRequest) =>
    apiClient.post<AuthResponse>('/auth/register', payload).then((r) => r.data),

  login: (payload: LoginRequest) =>
    apiClient.post<AuthResponse>('/auth/login', payload).then((r) => r.data),

  refresh: (refreshToken: string) =>
    apiClient.post<AuthResponse>('/auth/refresh', { refreshToken }).then((r) => r.data),

  me: () => apiClient.get<UserResponse>('/auth/me').then((r) => r.data),
};
