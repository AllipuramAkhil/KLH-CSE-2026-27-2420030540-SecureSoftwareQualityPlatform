import { apiClient } from './axios';
import type { SecurityVulnerabilityResponse } from '@/types/security';
import type {
  ZapImportResultResponse,
  ZapScanStatusResponse,
  ZapScanTriggerResponse,
  ZapStatusResponse,
} from '@/types/zap';

export const zapApi = {
  getStatus: () => apiClient.get<ZapStatusResponse>('/zap/status').then((r) => r.data),

  triggerBaselineScan: (projectId: number, targetUrl: string) =>
    apiClient
      .post<ZapScanTriggerResponse>(`/projects/${projectId}/zap/scan/baseline`, null, {
        params: { targetUrl },
      })
      .then((r) => r.data),

  getScanStatus: (scanId: string) =>
    apiClient.get<ZapScanStatusResponse>(`/zap/scan/${scanId}/status`).then((r) => r.data),

  collectResults: (projectId: number, targetUrl: string) =>
    apiClient
      .post<ZapImportResultResponse>(`/projects/${projectId}/zap/scan/results`, null, {
        params: { targetUrl },
      })
      .then((r) => r.data),

  importFromFile: (projectId: number, file: File) => {
    const formData = new FormData();
    formData.append('report', file);
    return apiClient
      .post<ZapImportResultResponse>(`/projects/${projectId}/zap/import-file`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  getFindings: (projectId: number) =>
    apiClient.get<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/zap`).then((r) => r.data),
};
