import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import {
  ApplicationStatus,
  InterviewMode,
  InterviewResult,
  InterviewSchedule,
  InterviewStatus,
  JobType,
  Paginated,
  PlacementApplication,
  PlacementDashboard,
  PlacementDrive,
  PlacementStatus,
} from "@/types";

export interface PlacementCreateInput {
  companyId?: string;
  companyName?: string;
  companyLogoUrl?: string;
  role: string;
  description?: string;
  eligibility?: string;
  skillsRequired?: string[];
  allowedBranches?: string[];
  salaryMin?: number;
  salaryMax?: number;
  minCgpa?: number;
  location?: string;
  jobType: JobType;
  driveDate?: string;
  lastDateToApply: string;
  applicationLink?: string;
}

export const placementsApi = {
  list: (params?: { page?: number; size?: number; status?: PlacementStatus; search?: string; companyId?: string }) =>
    apiClient.get<Paginated<PlacementDrive>>(API_ROUTES.placements, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<PlacementDrive>(`${API_ROUTES.placements}/${id}`).then((r) => r.data),

  create: (payload: PlacementCreateInput) =>
    apiClient.post<PlacementDrive>(API_ROUTES.placements, payload).then((r) => r.data),

  update: (id: string, payload: PlacementCreateInput) =>
    apiClient.put<PlacementDrive>(`${API_ROUTES.placements}/${id}`, payload).then((r) => r.data),

  setStatus: (id: string, status: PlacementStatus) =>
    apiClient.patch<PlacementDrive>(`${API_ROUTES.placements}/${id}/status`, null, { params: { status } }).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.placements}/${id}`).then((r) => r.data),

  apply: (id: string, resumeUrl?: string) =>
    apiClient.post<PlacementApplication>(API_ROUTES.placementApply(id), resumeUrl ? { resumeUrl } : {}).then((r) => r.data),

  myApplications: (params?: { page?: number; size?: number }) =>
    apiClient.get<Paginated<PlacementApplication>>(API_ROUTES.myApplications, { params }).then((r) => r.data),

  myInterviews: () => apiClient.get<InterviewSchedule[]>(API_ROUTES.myInterviews).then((r) => r.data),

  withdraw: (applicationId: string) =>
    apiClient.post(API_ROUTES.applicationWithdraw(applicationId)).then((r) => r.data),

  attachResume: (applicationId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<PlacementApplication>(API_ROUTES.applicationResume(applicationId), formData, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  listApplications: (placementId: string, params?: { page?: number; size?: number; status?: ApplicationStatus }) =>
    apiClient
      .get<Paginated<PlacementApplication>>(API_ROUTES.placementApplications(placementId), { params })
      .then((r) => r.data),

  updateApplicationStatus: (applicationId: string, status: ApplicationStatus, notes?: string) =>
    apiClient
      .patch<PlacementApplication>(API_ROUTES.applicationStatus(applicationId), { status, notes })
      .then((r) => r.data),

  listInterviews: (applicationId: string) =>
    apiClient.get<InterviewSchedule[]>(API_ROUTES.applicationInterviews(applicationId)).then((r) => r.data),

  scheduleInterview: (
    applicationId: string,
    payload: {
      roundNumber?: number;
      roundName: string;
      scheduledAt: string;
      mode: InterviewMode;
      venueOrLink?: string;
      interviewerName?: string;
    }
  ) => apiClient.post<InterviewSchedule>(API_ROUTES.applicationInterviews(applicationId), payload).then((r) => r.data),

  updateInterview: (
    interviewId: string,
    payload: {
      roundName: string;
      scheduledAt: string;
      mode: InterviewMode;
      venueOrLink?: string;
      interviewerName?: string;
      status: InterviewStatus;
      result: InterviewResult;
      feedback?: string;
    }
  ) => apiClient.put<InterviewSchedule>(API_ROUTES.interviewById(interviewId), payload).then((r) => r.data),

  deleteInterview: (interviewId: string) => apiClient.delete(API_ROUTES.interviewById(interviewId)).then((r) => r.data),

  dashboard: () => apiClient.get<PlacementDashboard>(API_ROUTES.placementDashboard).then((r) => r.data),
};
