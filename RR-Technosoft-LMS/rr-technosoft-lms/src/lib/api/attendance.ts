import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { AttendanceRecord, AttendanceSummary, Paginated } from "@/types";

export const attendanceApi = {
  mine: (params?: { from?: string; to?: string }) =>
    apiClient.get<AttendanceRecord[]>(`${API_ROUTES.attendance}/me`, { params }).then((r) => r.data),

  summary: () =>
    apiClient.get<AttendanceSummary>(`${API_ROUTES.attendance}/me/summary`).then((r) => r.data),

  list: (params?: { page?: number; size?: number; courseId?: string; date?: string }) =>
    apiClient.get<Paginated<AttendanceRecord>>(API_ROUTES.attendance, { params }).then((r) => r.data),

  mark: (payload: { studentId: string; courseId: string; date: string; status: string }) =>
    apiClient.post(API_ROUTES.attendance, payload).then((r) => r.data),

  bulkMark: (payload: { courseId: string; date: string; records: { studentId: string; status: string }[] }) =>
    apiClient.post(`${API_ROUTES.attendance}/bulk`, payload).then((r) => r.data),
};
