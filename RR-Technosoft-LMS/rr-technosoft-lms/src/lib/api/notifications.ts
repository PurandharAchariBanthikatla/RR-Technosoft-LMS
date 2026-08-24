import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { AppNotification, Paginated } from "@/types";

export const notificationsApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<Paginated<AppNotification>>(API_ROUTES.notifications, { params }).then((r) => r.data),

  unreadCount: () =>
    apiClient.get<{ count: number }>(`${API_ROUTES.notifications}/unread-count`).then((r) => r.data.count),

  markRead: (id: string) =>
    apiClient.patch<AppNotification>(`${API_ROUTES.notifications}/${id}/read`).then((r) => r.data),

  markAllRead: () => apiClient.patch(`${API_ROUTES.notifications}/read-all`),
};
