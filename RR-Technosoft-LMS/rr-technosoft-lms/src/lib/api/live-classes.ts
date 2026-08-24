import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { LiveClass, Paginated } from "@/types";

export const liveClassesApi = {
  list: (params?: { page?: number; size?: number; status?: string; courseId?: string }) =>
    apiClient.get<Paginated<LiveClass>>(API_ROUTES.liveClasses, { params }).then((r) => r.data),

  upcoming: () => apiClient.get<LiveClass[]>(`${API_ROUTES.liveClasses}/upcoming`).then((r) => r.data),

  create: (payload: Partial<LiveClass>) =>
    apiClient.post<LiveClass>(API_ROUTES.liveClasses, payload).then((r) => r.data),

  update: (id: string, payload: Partial<LiveClass>) =>
    apiClient.put<LiveClass>(`${API_ROUTES.liveClasses}/${id}`, payload).then((r) => r.data),
};
