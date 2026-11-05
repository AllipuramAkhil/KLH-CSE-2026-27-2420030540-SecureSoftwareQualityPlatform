import { apiClient } from './axios';
import type { CodeChurnAnalysisResponse, CodeChurnResponse, GitChurnStatusResponse } from '@/types/churn';

export const churnApi = {
  getStatus: () => apiClient.get<GitChurnStatusResponse>('/code-churn/status').then((r) => r.data),

  analyze: (projectId: number) =>
    apiClient.post<CodeChurnAnalysisResponse>(`/projects/${projectId}/code-churn/analyze`).then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<CodeChurnResponse[]>(`/projects/${projectId}/code-churn`).then((r) => r.data),
};
