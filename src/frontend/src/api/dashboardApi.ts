import { apiClient } from './axios';
import type { DashboardResponse } from '@/types/dashboard';

export const dashboardApi = {
  getDashboard: (projectId: number) =>
    apiClient.get<DashboardResponse>(`/projects/${projectId}/dashboard`).then((r) => r.data),
};
