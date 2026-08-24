import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import {
  DashboardAnalytics,
  Paginated,
  StudentReportRow,
  FacultyReportRow,
  AttendanceReportRow,
  AssignmentReportRow,
  RevenueReportRow,
  RevenueSummary,
} from "@/types";

const BASE = API_ROUTES.reports;

/** Triggers a browser download for a blob returned by an /export/* endpoint. */
function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

function stamp() {
  return new Date().toISOString().slice(0, 10);
}

async function fetchAndDownload(url: string, params: Record<string, unknown>, filenamePrefix: string, ext: string) {
  const response = await apiClient.get(url, { params, responseType: "blob" });
  downloadBlob(response.data as Blob, `${filenamePrefix}-${stamp()}.${ext}`);
}

export interface StudentReportFilters {
  search?: string;
  batch?: string;
  branch?: string;
  courseId?: string;
  page?: number;
  size?: number;
}

export interface FacultyReportFilters {
  search?: string;
  page?: number;
  size?: number;
}

export interface AttendanceReportFilters {
  courseId?: string;
  from?: string; // ISO date (yyyy-MM-dd)
  to?: string;
  page?: number;
  size?: number;
}

export interface AssignmentReportFilters {
  courseId?: string;
  from?: string; // ISO date-time
  to?: string;
  page?: number;
  size?: number;
}

export interface RevenueReportFilters {
  courseId?: string;
  from?: string; // ISO date-time
  to?: string;
  page?: number;
  size?: number;
}

export const reportsApi = {
  // Dashboard Analytics ------------------------------------------------
  dashboard: () => apiClient.get<DashboardAnalytics>(`${BASE}/dashboard`).then((r) => r.data),

  // Student Reports ------------------------------------------------------
  students: (filters: StudentReportFilters = {}) =>
    apiClient.get<Paginated<StudentReportRow>>(`${BASE}/students`, { params: filters }).then((r) => r.data),
  exportStudentsExcel: (filters: Omit<StudentReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/students/export/excel`, filters, "student-report", "xlsx"),
  exportStudentsPdf: (filters: Omit<StudentReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/students/export/pdf`, filters, "student-report", "pdf"),

  // Faculty Reports --------------------------------------------------------
  faculty: (filters: FacultyReportFilters = {}) =>
    apiClient.get<Paginated<FacultyReportRow>>(`${BASE}/faculty`, { params: filters }).then((r) => r.data),
  exportFacultyExcel: (filters: Omit<FacultyReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/faculty/export/excel`, filters, "faculty-report", "xlsx"),
  exportFacultyPdf: (filters: Omit<FacultyReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/faculty/export/pdf`, filters, "faculty-report", "pdf"),

  // Attendance Reports ------------------------------------------------------
  attendance: (filters: AttendanceReportFilters = {}) =>
    apiClient.get<Paginated<AttendanceReportRow>>(`${BASE}/attendance`, { params: filters }).then((r) => r.data),
  exportAttendanceExcel: (filters: Omit<AttendanceReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/attendance/export/excel`, filters, "attendance-report", "xlsx"),
  exportAttendancePdf: (filters: Omit<AttendanceReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/attendance/export/pdf`, filters, "attendance-report", "pdf"),

  // Assignment Reports -------------------------------------------------------
  assignments: (filters: AssignmentReportFilters = {}) =>
    apiClient.get<Paginated<AssignmentReportRow>>(`${BASE}/assignments`, { params: filters }).then((r) => r.data),
  exportAssignmentsExcel: (filters: Omit<AssignmentReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/assignments/export/excel`, filters, "assignment-report", "xlsx"),
  exportAssignmentsPdf: (filters: Omit<AssignmentReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/assignments/export/pdf`, filters, "assignment-report", "pdf"),

  // Revenue Reports ------------------------------------------------------
  revenue: (filters: RevenueReportFilters = {}) =>
    apiClient.get<Paginated<RevenueReportRow>>(`${BASE}/revenue`, { params: filters }).then((r) => r.data),
  revenueSummary: (params: { from?: string; to?: string } = {}) =>
    apiClient.get<RevenueSummary>(`${BASE}/revenue/summary`, { params }).then((r) => r.data),
  exportRevenueExcel: (filters: Omit<RevenueReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/revenue/export/excel`, filters, "revenue-report", "xlsx"),
  exportRevenuePdf: (filters: Omit<RevenueReportFilters, "page" | "size"> = {}) =>
    fetchAndDownload(`${BASE}/revenue/export/pdf`, filters, "revenue-report", "pdf"),
};
