import { apiClient } from './axios';
import type { AiAnalysisRequest, AiAnalysisResponse, AiGenerateTestsRequest } from '@/types/ai';

export const aiApi = {
  analyze: (projectId: number, payload: AiAnalysisRequest) =>
    apiClient.post<AiAnalysisResponse>(`/projects/${projectId}/ai/analyze`, payload).then((r) => r.data),

  generateTests: (projectId: number, payload: AiGenerateTestsRequest) =>
    apiClient.post<string[]>(`/projects/${projectId}/ai/generate-tests`, payload).then((r) => r.data),

  recommendations: (projectId: number) =>
    apiClient.post<AiAnalysisResponse>(`/projects/${projectId}/ai/recommendations`).then((r) => r.data),

  explainDefect: (projectId: number, qualityAnalysisId: number) =>
    apiClient
      .post<AiAnalysisResponse>(`/projects/${projectId}/ai/explain-defect`, { qualityAnalysisId })
      .then((r) => r.data),

  explainVulnerability: (projectId: number, vulnerabilityId: number) =>
    apiClient
      .post<AiAnalysisResponse>(`/projects/${projectId}/ai/explain-vulnerability`, { vulnerabilityId })
      .then((r) => r.data),
};
