import { apiClient } from './axios';
import type { NotificationResponse } from '@/types/notification';
import type { Page } from '@/types/common';

export const notificationApi = {
  getAll: (page = 0, size = 10) =>
    apiClient
      .get<Page<NotificationResponse>>('/notifications', { params: { page, size } })
      .then((r) => r.data),

  getUnread: () => apiClient.get<NotificationResponse[]>('/notifications/unread').then((r) => r.data),

  markRead: (id: number) => apiClient.patch<void>(`/notifications/${id}/read`).then((r) => r.data),

  markAllRead: () => apiClient.patch<void>('/notifications/read-all').then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/notifications/${id}`).then((r) => r.data),
};
