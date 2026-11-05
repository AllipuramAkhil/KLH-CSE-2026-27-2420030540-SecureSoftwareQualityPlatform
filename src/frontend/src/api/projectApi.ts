import { apiClient } from './axios';
import type { ProjectRequest, ProjectResponse } from '@/types/project';
import type { Page } from '@/types/common';

export interface ProjectListParams {
  page?: number;
  size?: number;
  sort?: string;
}

export const projectApi = {
  create: (payload: ProjectRequest) =>
    apiClient.post<ProjectResponse>('/projects', payload).then((r) => r.data),

  getAll: (params: ProjectListParams = {}) =>
    apiClient
      .get<Page<ProjectResponse>>('/projects', {
        params: { page: params.page ?? 0, size: params.size ?? 10, sort: params.sort ?? 'createdAt,desc' },
      })
      .then((r) => r.data),

  getById: (id: number) => apiClient.get<ProjectResponse>(`/projects/${id}`).then((r) => r.data),

  update: (id: number, payload: ProjectRequest) =>
    apiClient.put<ProjectResponse>(`/projects/${id}`, payload).then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/projects/${id}`).then((r) => r.data),
};
