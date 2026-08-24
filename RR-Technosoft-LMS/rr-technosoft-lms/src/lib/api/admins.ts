import { apiClient } from "./client";
import { API_ROUTES, AccountStatus } from "@/lib/constants";
import { Paginated, User, UserSummaryApiResponse } from "@/types";
import { mapUserSummaryToUser } from "./mappers";

export interface CreateAdminPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  department?: string;
  designation?: string;
}

/** Matches com.rrtechnosoft.lms.controller.AdminManagementController (/admins).
 *  Every call here requires the caller to hold the SUPER_ADMIN role. */
export const adminsApi = {
  list: async (params?: { page?: number; size?: number }): Promise<Paginated<User>> => {
    const { data } = await apiClient.get<Paginated<UserSummaryApiResponse>>(API_ROUTES.admins, { params });
    return { ...data, content: data.content.map(mapUserSummaryToUser) };
  },

  create: (payload: CreateAdminPayload) =>
    apiClient.post<UserSummaryApiResponse>(API_ROUTES.admins, payload).then((r) => mapUserSummaryToUser(r.data)),

  setStatus: (id: string, status: AccountStatus) =>
    apiClient
      .patch<UserSummaryApiResponse>(`${API_ROUTES.admins}/${id}/status`, null, { params: { status } })
      .then((r) => mapUserSummaryToUser(r.data)),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.admins}/${id}`),
};
