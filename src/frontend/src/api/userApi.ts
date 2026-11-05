import { apiClient } from './axios';
import type { UserResponse, UserUpdateRequest } from '@/types/user';

export const userApi = {
  getAll: () => apiClient.get<UserResponse[]>('/users').then((r) => r.data),

  getById: (id: number) => apiClient.get<UserResponse>(`/users/${id}`).then((r) => r.data),

  me: () => apiClient.get<UserResponse>('/users/me').then((r) => r.data),

  updateMe: (payload: UserUpdateRequest) =>
    apiClient.put<UserResponse>('/users/me', payload).then((r) => r.data),

  updateById: (id: number, payload: UserUpdateRequest) =>
    apiClient.put<UserResponse>(`/users/${id}`, payload).then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/users/${id}`).then((r) => r.data),
};
