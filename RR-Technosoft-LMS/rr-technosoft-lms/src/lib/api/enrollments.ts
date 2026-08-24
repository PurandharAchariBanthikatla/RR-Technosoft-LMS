import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Enrollment, Paginated } from "@/types";

export const enrollmentsApi = {
  list: (params?: { page?: number; size?: number; status?: string; courseId?: string }) =>
    apiClient.get<Paginated<Enrollment>>(API_ROUTES.enrollments, { params }).then((r) => r.data),

  mine: () => apiClient.get<Enrollment[]>(`${API_ROUTES.enrollments}/me`).then((r) => r.data),

  enroll: (courseId: string) =>
    apiClient.post<Enrollment>(API_ROUTES.enrollments, { courseId }).then((r) => r.data),

  updateStatus: (id: string, status: string) =>
    apiClient.patch<Enrollment>(`${API_ROUTES.enrollments}/${id}`, { status }).then((r) => r.data),
};
