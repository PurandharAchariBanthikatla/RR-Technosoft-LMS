import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { AdminDashboardStats, StudentDashboardStats } from "@/types";

export const dashboardApi = {
  admin: () => apiClient.get<AdminDashboardStats>(API_ROUTES.dashboardAdmin).then((r) => r.data),
  student: () => apiClient.get<StudentDashboardStats>(API_ROUTES.dashboardStudent).then((r) => r.data),
};
