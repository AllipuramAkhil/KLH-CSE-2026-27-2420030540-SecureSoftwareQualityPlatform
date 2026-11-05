import type { NotificationType } from './enums';

// Mirrors NotificationResponse.java
export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message?: string;
  isRead: boolean;
  createdAt: string;
}
