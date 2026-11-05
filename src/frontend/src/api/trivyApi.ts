import { apiClient } from './axios';
import type { SecurityVulnerabilityResponse } from '@/types/security';
import type { TrivyScanResultResponse, TrivyStatusResponse } from '@/types/trivy';

export const trivyApi = {
  getStatus: () => apiClient.get<TrivyStatusResponse>('/trivy/status').then((r) => r.data),

  scanFilesystem: (projectId: number, targetPath?: string) =>
    apiClient
      .post<TrivyScanResultResponse>(`/projects/${projectId}/trivy/scan/filesystem`, null, {
        params: targetPath ? { targetPath } : undefined,
      })
      .then((r) => r.data),

  scanImage: (projectId: number, imageRef: string) =>
    apiClient
      .post<TrivyScanResultResponse>(`/projects/${projectId}/trivy/scan/image`, null, { params: { imageRef } })
      .then((r) => r.data),

  importFromFile: (projectId: number, file: File) => {
    const formData = new FormData();
    formData.append('report', file);
    return apiClient
      .post<TrivyScanResultResponse>(`/projects/${projectId}/trivy/import-file`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  getFindings: (projectId: number) =>
    apiClient.get<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/trivy`).then((r) => r.data),
};
