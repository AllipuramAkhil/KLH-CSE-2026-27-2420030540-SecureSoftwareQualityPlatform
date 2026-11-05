import { apiClient } from './axios';
import type { SecurityVulnerabilityResponse } from '@/types/security';
import type { Severity, VulnerabilityStatus } from '@/types/enums';

export interface VulnerabilityFilterParams {
  severity?: Severity;
  status?: VulnerabilityStatus;
}

export const securityApi = {
  analyze: (projectId: number) =>
    apiClient
      .post<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/security/analyze`)
      .then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/security`).then((r) => r.data),

  getLatest: (projectId: number) =>
    apiClient
      .get<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/security/latest`)
      .then((r) => r.data),

  getVulnerabilities: (projectId: number, params: VulnerabilityFilterParams = {}) =>
    apiClient
      .get<SecurityVulnerabilityResponse[]>(`/projects/${projectId}/security/vulnerabilities`, { params })
      .then((r) => r.data),

  getById: (id: number) =>
    apiClient.get<SecurityVulnerabilityResponse>(`/vulnerabilities/${id}`).then((r) => r.data),

  updateStatus: (id: number, status: VulnerabilityStatus) =>
    apiClient
      .patch<SecurityVulnerabilityResponse>(`/vulnerabilities/${id}/status`, { status })
      .then((r) => r.data),
};
