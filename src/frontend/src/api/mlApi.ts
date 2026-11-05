import { apiClient } from './axios';
import type { ModelInfoResponse, ModelMetricsResponse, PredictionHistoryEntry, RetrainResponse } from '@/types/mlops';

export const mlApi = {
  getStatus: () => apiClient.get<{ available: boolean }>('/ml/status').then((r) => r.data),

  getModelInfo: () => apiClient.get<ModelInfoResponse>('/ml/model-info').then((r) => r.data),

  getMetrics: () => apiClient.get<ModelMetricsResponse>('/ml/metrics').then((r) => r.data),

  retrain: (compareLogreg = false) =>
    apiClient
      .post<RetrainResponse>('/ml/retrain', null, { params: { compareLogreg } })
      .then((r) => r.data),

  getPredictionHistory: (projectId?: number, limit = 50) =>
    apiClient
      .get<PredictionHistoryEntry[]>('/ml/prediction-history', { params: { projectId, limit } })
      .then((r) => r.data),
};
