import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Paginated, Quiz, QuizQuestion } from "@/types";

export const quizzesApi = {
  list: (params?: { page?: number; size?: number; courseId?: string }) =>
    apiClient.get<Paginated<Quiz>>(API_ROUTES.quizzes, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<Quiz>(`${API_ROUTES.quizzes}/${id}`).then((r) => r.data),

  questions: (id: string) =>
    apiClient.get<QuizQuestion[]>(`${API_ROUTES.quizzes}/${id}/questions`).then((r) => r.data),

  create: (payload: Partial<Quiz>) =>
    apiClient.post<Quiz>(API_ROUTES.quizzes, payload).then((r) => r.data),

  submit: (id: string, answers: { questionId: string; optionIndex: number }[]) =>
    apiClient.post(`${API_ROUTES.quizzes}/${id}/submit`, { answers }).then((r) => r.data),
};
