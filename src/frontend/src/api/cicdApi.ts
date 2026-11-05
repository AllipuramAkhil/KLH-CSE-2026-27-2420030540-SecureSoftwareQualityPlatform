import { apiClient } from './axios';
import type { CiCdPipelineResponse, CiCdWebhookRequest } from '@/types/cicd';

export const cicdApi = {
  webhook: (payload: CiCdWebhookRequest) =>
    apiClient.post<CiCdPipelineResponse>('/cicd/webhook', payload).then((r) => r.data),

  getById: (id: number) => apiClient.get<CiCdPipelineResponse>(`/cicd/${id}`).then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<CiCdPipelineResponse[]>(`/projects/${projectId}/cicd`).then((r) => r.data),

  trigger: (projectId: number) =>
    apiClient.post<CiCdPipelineResponse>(`/projects/${projectId}/cicd/trigger`).then((r) => r.data),
};
