import { apiClient } from './axios';
import type { TestExecutionRequest, TestExecutionResponse } from '@/types/testing';

export const testExecutionApi = {
  execute: (testId: number, payload: TestExecutionRequest) =>
    apiClient.post<TestExecutionResponse>(`/tests/${testId}/execute`, payload).then((r) => r.data),

  getByTestCase: (testId: number) =>
    apiClient.get<TestExecutionResponse[]>(`/tests/${testId}/executions`).then((r) => r.data),

  getById: (id: number) => apiClient.get<TestExecutionResponse>(`/executions/${id}`).then((r) => r.data),

  getByProject: (projectId: number) =>
    apiClient.get<TestExecutionResponse[]>(`/projects/${projectId}/executions`).then((r) => r.data),
};
