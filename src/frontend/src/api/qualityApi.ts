import { apiClient } from './axios';
import type { QualityAnalysisResponse } from '@/types/quality';

export const qualityApi = {
  analyze: (projectId: number) =>
    apiClient.post<QualityAnalysisResponse>(`/projects/${projectId}/quality/analyze`).then((r) => r.data),

  getHistory: (projectId: number) =>
    apiClient.get<QualityAnalysisResponse[]>(`/projects/${projectId}/quality`).then((r) => r.data),

  getLatest: (projectId: number) =>
    apiClient.get<QualityAnalysisResponse>(`/projects/${projectId}/quality/latest`).then((r) => r.data),

  getById: (projectId: number, analysisId: number) =>
    apiClient
      .get<QualityAnalysisResponse>(`/projects/${projectId}/quality/${analysisId}`)
      .then((r) => r.data),
};
