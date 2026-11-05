import { apiClient } from './axios';
import type { RiskAssessmentResponse, RiskSummaryResponse } from '@/types/risk';

export const riskApi = {
  analyze: (projectId: number) =>
    apiClient.post<RiskAssessmentResponse[]>(`/projects/${projectId}/risk/analyze`).then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<RiskAssessmentResponse[]>(`/projects/${projectId}/risk`).then((r) => r.data),

  getById: (projectId: number, id: number) =>
    apiClient.get<RiskAssessmentResponse>(`/projects/${projectId}/risk/${id}`).then((r) => r.data),

  getSummary: (projectId: number) =>
    apiClient.get<RiskSummaryResponse>(`/projects/${projectId}/risk/summary`).then((r) => r.data),
};
