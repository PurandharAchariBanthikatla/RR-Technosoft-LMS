export const API_BASE_URL = "http://18.170.40.18:8080/api/v1";
export const ACCESS_TOKEN_KEY = "rr_lms_access_token";
export const REFRESH_TOKEN_KEY = "rr_lms_refresh_token";

// Matches com.rrtechnosoft.lms.entity.enums.UserRole exactly — do not rename.
export enum Role {
  SUPER_ADMIN = "SUPER_ADMIN",
  ADMIN = "ADMIN",
  STUDENT = "STUDENT",
}

export const ROLE_HOME: Record<Role, string> = {
  [Role.SUPER_ADMIN]: "/admin/dashboard",
  [Role.ADMIN]: "/admin/dashboard",
  [Role.STUDENT]: "/student/dashboard",
};

// Matches com.rrtechnosoft.lms.entity.enums.AccountStatus exactly.
export enum AccountStatus {
  ACTIVE = "ACTIVE",
  INACTIVE = "INACTIVE",
  SUSPENDED = "SUSPENDED",
  PENDING = "PENDING",
}

// Backend implements: auth, admin/student management, courses (+modules/
// lessons), enrollments, assignments, quizzes, live-classes, attendance,
// certificates, notifications, the full Learning (resources/videos) and
// Placement (companies/drives/applications/interviews) modules, the
// Administration module (permissions/system-settings/organization-profile/
// master-data/feature-toggles/notification-settings/security-settings/backup),
// Reports & Analytics, and the Finance module (fee structures, student fees,
// payments, receipts, finance reports).
export const API_ROUTES = {
  auth: {
    login: "/auth/login",
    refresh: "/auth/refresh",
    logout: "/auth/logout",
  },
  admins: "/admins", // SUPER_ADMIN only
  users: "/users", // any authenticated user — self-service profile (GET/PATCH /users/me)
  studentsManage: "/students/manage", // ADMIN + SUPER_ADMIN — create/list/update students
  courses: "/courses",
  modules: (courseId: string) => `/courses/${courseId}/modules`,
  lessons: (moduleId: string) => `/modules/${moduleId}/lessons`,
  enrollments: "/enrollments",
  assignments: "/assignments",
  dailyTasks: "/daily-tasks",
  quizzes: "/quizzes",
  liveClasses: "/live-classes",
  attendance: "/attendance",
  certificates: "/certificates",
  notifications: "/notifications",

  // Learning module
  learningResources: "/learning-resources",
  learningResourceFile: (id: string) => `/learning-resources/${id}/file`,
  learningResourceDownload: (id: string) => `/learning-resources/${id}/download`,
  videos: "/videos",
  videoFile: (id: string) => `/videos/${id}/file`,
  videoView: (id: string) => `/videos/${id}/view`,

  // Placement module
  companies: "/companies",
  placements: "/placements",
  placementDashboard: "/placements/dashboard",
  placementApply: (id: string) => `/placements/${id}/apply`,
  placementApplications: (id: string) => `/placements/${id}/applications`,
  myApplications: "/placements/my-applications",
  applicationById: (applicationId: string) => `/placements/applications/${applicationId}`,
  applicationStatus: (applicationId: string) => `/placements/applications/${applicationId}/status`,
  applicationWithdraw: (applicationId: string) => `/placements/applications/${applicationId}/withdraw`,
  applicationResume: (applicationId: string) => `/placements/applications/${applicationId}/resume`,
  applicationInterviews: (applicationId: string) => `/placements/applications/${applicationId}/interviews`,
  myInterviews: "/placements/my-interviews",
  interviewById: (interviewId: string) => `/placements/interviews/${interviewId}`,

  practiceProblems: "/practice/problems",
  practiceSubmissions: "/practice/submissions",
  chatbotConversations: "/chatbot/conversations",
  chatbotMessages: "/chatbot/messages",
  chatbotConversationMessages: (conversationId: string) => `/chatbot/conversations/${conversationId}/messages`,
  chatbotConversationById: (conversationId: string) => `/chatbot/conversations/${conversationId}`,
  dashboardAdmin: "/dashboard/admin",
  dashboardStudent: "/dashboard/student",
  reports: "/reports",

  // Finance module — matches com.rrtechnosoft.lms.controller.{FeeStructureController,
  // StudentFeeController, PaymentController, ReceiptController, FinanceReportController}
  feeStructures: "/finance/fee-structures",
  studentFees: "/finance/student-fees",
  payments: "/finance/payments",
  receipts: "/finance/receipts",
  financeReports: "/finance/reports",

  // Administration module — matches PermissionController / SystemSettingController /
  // OrganizationProfileController / MasterDataController / FeatureToggleController /
  // NotificationSettingsController / SecuritySettingsController / BackupController.
  // All writes are SUPER_ADMIN only; feature-toggles and master-data GETs are open
  // to any authenticated user (see SecurityConfig).
  permissionMatrix: "/administration/permissions/matrix",
  systemSettings: "/system-settings",
  organizationProfile: "/administration/organization-profile",
  masterDataCategories: "/administration/master-data/categories",
  masterDataItems: (categoryId: string) => `/administration/master-data/categories/${categoryId}/items`,
  masterDataItem: (itemId: string) => `/administration/master-data/items/${itemId}`,
  featureToggles: "/administration/feature-toggles",
  notificationSettings: "/administration/notification-settings",
  securitySettings: "/administration/security-settings",
  backupConfig: "/administration/backup/config",
  backupRuns: "/administration/backup/runs",
  auditLogs: "/administration/audit-logs",
};
