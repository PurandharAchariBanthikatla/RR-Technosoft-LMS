import { apiClient } from "./client";
import { API_ROUTES, AccountStatus } from "@/lib/constants";
import { Paginated, User, UserSummaryApiResponse } from "@/types";
import { mapUserSummaryToUser } from "./mappers";

export interface CreateStudentPayload {
  fullName: string;
  phone?: string;
  initialPassword: string;
  batch?: string;
  branch?: string;
  college?: string;
  graduationYear?: number;
}

export interface UpdateStudentPayload {
  fullName: string;
  phone?: string;
  batch?: string;
  branch?: string;
  college?: string;
  graduationYear?: number;
}

/** Matches com.rrtechnosoft.lms.controller.StudentManagementController (/students/manage). */
export const studentsApi = {
  list: async (params?: { page?: number; size?: number; search?: string }): Promise<Paginated<User>> => {
    const { data } = await apiClient.get<Paginated<UserSummaryApiResponse>>(API_ROUTES.studentsManage, { params });
    return { ...data, content: data.content.map(mapUserSummaryToUser) };
  },

  create: (payload: CreateStudentPayload) =>
    apiClient.post<UserSummaryApiResponse>(API_ROUTES.studentsManage, payload).then((r) => mapUserSummaryToUser(r.data)),

  update: (id: string, payload: UpdateStudentPayload) =>
    apiClient
      .put<UserSummaryApiResponse>(`${API_ROUTES.studentsManage}/${id}`, payload)
      .then((r) => mapUserSummaryToUser(r.data)),

  setStatus: (id: string, status: AccountStatus) =>
    apiClient
      .patch<UserSummaryApiResponse>(`${API_ROUTES.studentsManage}/${id}/status`, null, { params: { status } })
      .then((r) => mapUserSummaryToUser(r.data)),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.studentsManage}/${id}`),
};
