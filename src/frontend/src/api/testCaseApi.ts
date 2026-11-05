import { apiClient } from './axios';
import type { TestCaseRequest, TestCaseResponse } from '@/types/testing';
import type { Page } from '@/types/common';
import type { TestType, TestCaseStatus } from '@/types/enums';

export interface TestCaseListParams {
  page?: number;
  size?: number;
  sort?: string;
  type?: TestType;
  priority?: string;
  status?: TestCaseStatus;
}

export const testCaseApi = {
  create: (projectId: number, payload: TestCaseRequest) =>
    apiClient.post<TestCaseResponse>(`/projects/${projectId}/tests`, payload).then((r) => r.data),

  getByProject: (projectId: number, params: TestCaseListParams = {}) =>
    apiClient
      .get<Page<TestCaseResponse>>(`/projects/${projectId}/tests`, {
        params: { page: params.page ?? 0, size: params.size ?? 10, ...params },
      })
      .then((r) => r.data),

  getById: (id: number) => apiClient.get<TestCaseResponse>(`/tests/${id}`).then((r) => r.data),

  update: (id: number, payload: TestCaseRequest) =>
    apiClient.put<TestCaseResponse>(`/tests/${id}`, payload).then((r) => r.data),

  updateStatus: (id: number, status: TestCaseStatus) =>
    apiClient.patch<TestCaseResponse>(`/tests/${id}/status`, { status }).then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/tests/${id}`).then((r) => r.data),
};
