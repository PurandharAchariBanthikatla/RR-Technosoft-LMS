import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { Company, Paginated } from "@/types";

export interface CompanyInput {
  name: string;
  logoUrl?: string;
  website?: string;
  industry?: string;
  description?: string;
  contactPersonName?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  isActive?: boolean;
}

export const companiesApi = {
  list: (params?: { page?: number; size?: number; search?: string; isActive?: boolean }) =>
    apiClient.get<Paginated<Company>>(API_ROUTES.companies, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<Company>(`${API_ROUTES.companies}/${id}`).then((r) => r.data),

  create: (payload: CompanyInput) => apiClient.post<Company>(API_ROUTES.companies, payload).then((r) => r.data),

  update: (id: string, payload: CompanyInput) =>
    apiClient.put<Company>(`${API_ROUTES.companies}/${id}`, payload).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.companies}/${id}`).then((r) => r.data),
};
