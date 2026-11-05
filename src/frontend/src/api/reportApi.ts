import { apiClient } from './axios';
import type { ReportResponse } from '@/types/report';

export const reportApi = {
  generate: (projectId: number, title?: string) =>
    apiClient
      .post<ReportResponse>(`/projects/${projectId}/reports`, title ? { title } : {})
      .then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<ReportResponse[]>(`/projects/${projectId}/reports`).then((r) => r.data),

  getById: (id: number) => apiClient.get<ReportResponse>(`/reports/${id}`).then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/reports/${id}`).then((r) => r.data),
};
