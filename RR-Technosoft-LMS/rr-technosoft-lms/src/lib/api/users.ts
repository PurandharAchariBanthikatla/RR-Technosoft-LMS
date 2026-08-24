import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { UserSummaryApiResponse } from "@/types";
import { mapUserSummaryToUser } from "./mappers";

export interface UpdateProfilePayload {
  fullName: string;
  phone?: string;
}

/** Matches com.rrtechnosoft.lms.controller.UserController (/users/me). */
export const usersApi = {
  me: () => apiClient.get<UserSummaryApiResponse>(`${API_ROUTES.users}/me`).then((r) => mapUserSummaryToUser(r.data)),

  updateMe: (payload: UpdateProfilePayload) =>
    apiClient.patch<UserSummaryApiResponse>(`${API_ROUTES.users}/me`, payload).then((r) => mapUserSummaryToUser(r.data)),
};
