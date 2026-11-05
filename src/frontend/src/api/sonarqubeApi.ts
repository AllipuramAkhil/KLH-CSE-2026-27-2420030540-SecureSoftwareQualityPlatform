import { apiClient } from './axios';
import type { SonarQubeAnalysisResponse, SonarQubeStatusResponse } from '@/types/sonarqube';

export const sonarQubeApi = {
  getStatus: () => apiClient.get<SonarQubeStatusResponse>('/sonarqube/status').then((r) => r.data),

  importLatest: (projectId: number, sonarProjectKey?: string) =>
    apiClient
      .post<SonarQubeAnalysisResponse>(`/projects/${projectId}/sonarqube/import`, null, {
        params: sonarProjectKey ? { sonarProjectKey } : undefined,
      })
      .then((r) => r.data),

  getLatest: (projectId: number) =>
    apiClient.get<SonarQubeAnalysisResponse>(`/projects/${projectId}/sonarqube/latest`).then((r) => r.data),

  getHistory: (projectId: number) =>
    apiClient.get<SonarQubeAnalysisResponse[]>(`/projects/${projectId}/sonarqube`).then((r) => r.data),
};
