import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Assignment, DailyTask, Paginated } from "@/types";

export const assignmentsApi = {
  list: (params?: { page?: number; size?: number; courseId?: string }) =>
    apiClient.get<Paginated<Assignment>>(API_ROUTES.assignments, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<Assignment>(`${API_ROUTES.assignments}/${id}`).then((r) => r.data),

  create: (payload: Partial<Assignment>) =>
    apiClient.post<Assignment>(API_ROUTES.assignments, payload).then((r) => r.data),

  submit: (id: string, payload: { fileUrl?: string; text?: string }) =>
    apiClient.post(`${API_ROUTES.assignments}/${id}/submit`, payload).then((r) => r.data),

  grade: (id: string, submissionId: string, score: number, feedback?: string) =>
    apiClient
      .patch(`${API_ROUTES.assignments}/${id}/submissions/${submissionId}`, { score, feedback })
      .then((r) => r.data),
};

export const dailyTasksApi = {
  list: (params?: { date?: string }) =>
    apiClient.get<DailyTask[]>(API_ROUTES.dailyTasks, { params }).then((r) => r.data),

  toggle: (id: string, completed: boolean) =>
    apiClient.patch<DailyTask>(`${API_ROUTES.dailyTasks}/${id}`, { completed }).then((r) => r.data),

  create: (payload: Partial<DailyTask>) =>
    apiClient.post<DailyTask>(API_ROUTES.dailyTasks, payload).then((r) => r.data),
};
