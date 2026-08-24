import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { LearningResource, Paginated, ResourceType } from "@/types";

export interface LearningResourceInput {
  title: string;
  description?: string;
  resourceType: ResourceType;
  category?: string;
  courseId?: string;
  externalUrl?: string;
  isPublished?: boolean;
}

export const learningResourcesApi = {
  list: (params?: { page?: number; size?: number; search?: string; category?: string; type?: ResourceType; courseId?: string }) =>
    apiClient.get<Paginated<LearningResource>>(API_ROUTES.learningResources, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<LearningResource>(`${API_ROUTES.learningResources}/${id}`).then((r) => r.data),

  create: (payload: LearningResourceInput) =>
    apiClient.post<LearningResource>(API_ROUTES.learningResources, payload).then((r) => r.data),

  update: (id: string, payload: LearningResourceInput) =>
    apiClient.put<LearningResource>(`${API_ROUTES.learningResources}/${id}`, payload).then((r) => r.data),

  uploadFile: (id: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<LearningResource>(API_ROUTES.learningResourceFile(id), formData, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  recordDownload: (id: string) => apiClient.post(API_ROUTES.learningResourceDownload(id)).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.learningResources}/${id}`).then((r) => r.data),
};
