import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Paginated, VideoResource, VideoSource } from "@/types";

export interface VideoResourceInput {
  title: string;
  description?: string;
  category?: string;
  courseId?: string;
  source: VideoSource;
  videoUrl?: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  isPublished?: boolean;
}

export const videosApi = {
  list: (params?: { page?: number; size?: number; search?: string; category?: string; courseId?: string }) =>
    apiClient.get<Paginated<VideoResource>>(API_ROUTES.videos, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<VideoResource>(`${API_ROUTES.videos}/${id}`).then((r) => r.data),

  create: (payload: VideoResourceInput) => apiClient.post<VideoResource>(API_ROUTES.videos, payload).then((r) => r.data),

  update: (id: string, payload: VideoResourceInput) =>
    apiClient.put<VideoResource>(`${API_ROUTES.videos}/${id}`, payload).then((r) => r.data),

  uploadFile: (id: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<VideoResource>(API_ROUTES.videoFile(id), formData, { headers: { "Content-Type": "multipart/form-data" } })
      .then((r) => r.data);
  },

  recordView: (id: string) => apiClient.post(API_ROUTES.videoView(id)).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.videos}/${id}`).then((r) => r.data),
};
