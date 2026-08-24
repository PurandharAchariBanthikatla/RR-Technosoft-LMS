import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Course, CourseModule, Lesson, Paginated } from "@/types";

export const coursesApi = {
  list: (params?: { page?: number; size?: number; search?: string; category?: string; status?: string }) =>
    apiClient.get<Paginated<Course>>(API_ROUTES.courses, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<Course>(`${API_ROUTES.courses}/${id}`).then((r) => r.data),

  create: (payload: Partial<Course>) =>
    apiClient.post<Course>(API_ROUTES.courses, payload).then((r) => r.data),

  update: (id: string, payload: Partial<Course>) =>
    apiClient.put<Course>(`${API_ROUTES.courses}/${id}`, payload).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.courses}/${id}`),

  setStatus: (id: string, status: "DRAFT" | "PUBLISHED" | "ARCHIVED") =>
    apiClient
      .patch<Course>(`${API_ROUTES.courses}/${id}/status`, null, { params: { status } })
      .then((r) => r.data),

  modules: (courseId: string) =>
    apiClient.get<CourseModule[]>(API_ROUTES.modules(courseId)).then((r) => r.data),

  createModule: (courseId: string, payload: Partial<CourseModule>) =>
    apiClient.post<CourseModule>(API_ROUTES.modules(courseId), payload).then((r) => r.data),

  lessons: (moduleId: string) =>
    apiClient.get<Lesson[]>(API_ROUTES.lessons(moduleId)).then((r) => r.data),

  createLesson: (moduleId: string, payload: Partial<Lesson>) =>
    apiClient.post<Lesson>(API_ROUTES.lessons(moduleId), payload).then((r) => r.data),
};
