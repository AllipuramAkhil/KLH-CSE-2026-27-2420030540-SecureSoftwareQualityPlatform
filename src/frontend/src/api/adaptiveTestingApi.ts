import { apiClient } from './axios';
import type { AdaptiveTestingResponse, AnalysisHistoryEntry } from '@/types/adaptive';

export const adaptiveTestingApi = {
  prioritize: (projectId: number) =>
    apiClient
      .post<AdaptiveTestingResponse>(`/projects/${projectId}/adaptive-testing/prioritize`)
      .then((r) => r.data),

  getRecommendations: (projectId: number) =>
    apiClient
      .get<AdaptiveTestingResponse>(`/projects/${projectId}/adaptive-testing/recommendations`)
      .then((r) => r.data),

  submitFeedback: (projectId: number, summary: string) =>
    apiClient
      .post<void>(`/projects/${projectId}/adaptive-testing/feedback`, { summary })
      .then((r) => r.data),

  getHistory: (projectId: number) =>
    apiClient
      .get<AnalysisHistoryEntry[]>(`/projects/${projectId}/adaptive-testing/history`)
      .then((r) => r.data),
};
