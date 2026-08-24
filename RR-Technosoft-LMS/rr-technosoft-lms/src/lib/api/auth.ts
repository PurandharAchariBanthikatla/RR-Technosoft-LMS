import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { AuthApiResponse } from "@/types";

/**
 * Matches com.rrtechnosoft.lms.controller.AuthController exactly:
 * - login: identifier is an email for SUPER_ADMIN/ADMIN, or a Student ID
 *   (e.g. RRT2026S0001) for STUDENT — the backend detects which by format.
 * - There is no self-registration endpoint. Admins create student/admin
 *   accounts via lib/api/students.ts and lib/api/admins.ts.
 * - There is no /auth/me — the access token itself carries role/email/
 *   fullName/studentId claims, decoded client-side in hooks/use-auth.ts.
 */
export const authApi = {
  login: (payload: { identifier: string; password: string }) =>
    apiClient.post<AuthApiResponse>(API_ROUTES.auth.login, payload).then((r) => r.data),

  refresh: (refreshToken: string) =>
    apiClient.post<AuthApiResponse>(API_ROUTES.auth.refresh, { refreshToken }).then((r) => r.data),

  logout: () => apiClient.post(API_ROUTES.auth.logout),
};
