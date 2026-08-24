import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Certificate, Paginated } from "@/types";

export const certificatesApi = {
  mine: () => apiClient.get<Certificate[]>(`${API_ROUTES.certificates}/me`).then((r) => r.data),

  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<Paginated<Certificate>>(API_ROUTES.certificates, { params }).then((r) => r.data),

  issue: (payload: { studentId: string; courseId: string }) =>
    apiClient.post<Certificate>(API_ROUTES.certificates, payload).then((r) => r.data),

  verify: (code: string) =>
    apiClient.get<Certificate>(`${API_ROUTES.certificates}/verify/${code}`).then((r) => r.data),
};
