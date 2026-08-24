import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Paginated, PracticeProblem } from "@/types";

export const practiceApi = {
  list: (params?: { page?: number; size?: number; difficulty?: string; topic?: string; search?: string }) =>
    apiClient.get<Paginated<PracticeProblem>>(API_ROUTES.practiceProblems, { params }).then((r) => r.data),

  get: (id: string) =>
    apiClient.get<PracticeProblem>(`${API_ROUTES.practiceProblems}/${id}`).then((r) => r.data),

  submit: (id: string, payload: { language: string; code: string }) =>
    apiClient.post(API_ROUTES.practiceSubmissions, { problemId: id, ...payload }).then((r) => r.data),

  mySubmissions: (id: string) =>
    apiClient.get(`${API_ROUTES.practiceSubmissions}?problemId=${id}`).then((r) => r.data),
};
