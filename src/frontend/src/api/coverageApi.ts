import { apiClient } from './axios';
import type { CoverageReportResponse, CoverageTrendPoint } from '@/types/coverage';

export const coverageApi = {
  importFromDefaultPath: (projectId: number) =>
    apiClient.post<CoverageReportResponse>(`/projects/${projectId}/coverage/import`).then((r) => r.data),

  importFromFile: (projectId: number, file: File) => {
    const formData = new FormData();
    formData.append('report', file);
    return apiClient
      .post<CoverageReportResponse>(`/projects/${projectId}/coverage/import-file`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  getLatest: (projectId: number) =>
    apiClient.get<CoverageReportResponse>(`/projects/${projectId}/coverage/latest`).then((r) => r.data),

  getHistory: (projectId: number) =>
    apiClient.get<CoverageReportResponse[]>(`/projects/${projectId}/coverage`).then((r) => r.data),

  getTrend: (projectId: number) =>
    apiClient.get<CoverageTrendPoint[]>(`/projects/${projectId}/coverage/trend`).then((r) => r.data),
};
