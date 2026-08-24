import { Role, AccountStatus } from "@/lib/constants";

/** Internal, UI-friendly user shape used throughout the app. Built by mapping
 *  the backend's flat AuthResponse / UserSummaryResponse via lib/api/mappers.ts. */
export interface User {
  id: string;
  name: string;
  email?: string;
  studentId?: string;
  role: Role;
  avatarUrl?: string;
  status?: AccountStatus;
  phone?: string;
  lastLoginAt?: string;
  createdAt: string;
}

/** Raw shape of POST /auth/login, /auth/refresh — com.rrtechnosoft.lms.dto.response.AuthResponse */
export interface AuthApiResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: Role;
  fullName: string;
  email: string | null;
  studentId: string | null;
}

/** Raw shape returned by /students/manage, /admins — com.rrtechnosoft.lms.dto.response.UserSummaryResponse */
export interface UserSummaryApiResponse {
  id: string;
  role: Role;
  email: string | null;
  studentId: string | null;
  fullName: string;
  phone: string | null;
  status: AccountStatus;
  lastLoginAt: string | null;
  createdAt: string;
}

/** Matches Spring Data's default Page<T> JSON serialization (note: `number`, not `page`). */
export interface Paginated<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type CourseLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type CourseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export interface Course {
  id: string;
  title: string;
  slug: string;
  description: string;
  thumbnailUrl?: string;
  category: string;
  level: CourseLevel;
  status: CourseStatus;
  durationWeeks: number;
  instructorName: string;
  price: number;
  studentsEnrolled: number;
  rating?: number;
  moduleCount: number;
  createdAt: string;
}

export interface CourseModule {
  id: string;
  courseId: string;
  title: string;
  order: number;
  lessonCount: number;
}

export type LessonType = "VIDEO" | "ARTICLE" | "RESOURCE";

export interface Lesson {
  id: string;
  moduleId: string;
  title: string;
  type: LessonType;
  durationMinutes: number;
  order: number;
  completed?: boolean;
  contentUrl?: string;
}

export type EnrollmentStatus = "ACTIVE" | "COMPLETED" | "DROPPED" | "PENDING";

export interface Enrollment {
  id: string;
  courseId: string;
  courseTitle: string;
  studentId: string;
  studentName: string;
  status: EnrollmentStatus;
  progress: number;
  enrolledAt: string;
}

export type SubmissionStatus = "NOT_SUBMITTED" | "SUBMITTED" | "GRADED" | "LATE";

export interface Assignment {
  id: string;
  courseId: string;
  courseTitle: string;
  title: string;
  description: string;
  dueDate: string;
  maxScore: number;
  status: SubmissionStatus;
  score?: number;
  submittedCount?: number;
  totalStudents?: number;
}

export interface DailyTask {
  id: string;
  title: string;
  description: string;
  date: string;
  completed: boolean;
  courseTitle?: string;
}

export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  correctOptionIndex?: number;
}

export interface Quiz {
  id: string;
  courseId: string;
  courseTitle: string;
  title: string;
  durationMinutes: number;
  totalQuestions: number;
  totalMarks: number;
  attempted: boolean;
  score?: number;
  availableFrom: string;
  availableTo: string;
}

export type LiveClassStatus = "SCHEDULED" | "LIVE" | "ENDED" | "CANCELLED";

export interface LiveClass {
  id: string;
  courseId: string;
  courseTitle: string;
  title: string;
  instructorName: string;
  startTime: string;
  endTime: string;
  status: LiveClassStatus;
  meetingUrl?: string;
  recordingUrl?: string;
}

export type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED";

export interface AttendanceRecord {
  id: string;
  date: string;
  courseTitle: string;
  status: AttendanceStatus;
  studentId?: string;
  studentName?: string;
}

export interface AttendanceSummary {
  totalClasses: number;
  present: number;
  absent: number;
  late: number;
  percentage: number;
}

export interface Certificate {
  id: string;
  courseTitle: string;
  studentName: string;
  issuedAt: string;
  certificateUrl: string;
  verificationCode: string;
}
// Drive lifecycle status (backend `placement_status`). Kept as its own type,
// separate from ApplicationStatus below — a single PlacementStatus union
// mixing both used to exist here; drive and application status are distinct
// state machines on the backend, so they get distinct frontend types too.
export type PlacementStatus = "DRAFT" | "OPEN" | "CLOSED" | "COMPLETED" | "CANCELLED";

export type JobType = "FULL_TIME" | "INTERNSHIP" | "PART_TIME" | "CONTRACT";

export type ApplicationStatus =
  | "APPLIED"
  | "SHORTLISTED"
  | "INTERVIEW_SCHEDULED"
  | "SELECTED"
  | "REJECTED"
  | "WITHDRAWN";

export type InterviewMode = "ONLINE" | "OFFLINE" | "TELEPHONIC";
export type InterviewStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "RESCHEDULED";
export type InterviewResult = "PENDING" | "PASS" | "FAIL" | "ON_HOLD";

/** Raw shape of GET/POST /companies — com.rrtechnosoft.lms.dto.response.CompanyResponse */
export interface Company {
  id: string;
  name: string;
  logoUrl?: string;
  website?: string;
  industry?: string;
  description?: string;
  contactPersonName?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  isActive: boolean;
  activeDriveCount: number;
  createdAt: string;
}

/**
 * Raw shape of GET/POST /placements — com.rrtechnosoft.lms.dto.response.PlacementResponse.
 * companyName/role/location/packageLpa/eligibility/driveDate/status/applicantsCount
 * were the original contract fields; everything else is additive.
 */
export interface PlacementDrive {
  id: string;
  companyId?: string;
  companyName: string;
  companyLogoUrl?: string;
  role: string;
  description?: string;
  eligibility: string;
  skillsRequired?: string[];
  allowedBranches?: string[];
  salaryMin?: number;
  salaryMax?: number;
  packageLpa: string;
  minCgpa?: number;
  location: string;
  jobType: JobType;
  driveDate: string;
  lastDateToApply: string;
  applicationLink?: string;
  status: PlacementStatus;
  applicantsCount?: number;
  createdAt: string;
}

/** Raw shape of GET /placements/{id}/applications, /placements/my-applications — PlacementApplicationResponse */
export interface PlacementApplication {
  id: string;
  placementId: string;
  companyName: string;
  role: string;
  studentId: string;
  studentName: string;
  studentIdNumber?: string;
  status: ApplicationStatus;
  resumeUrl?: string;
  notes?: string;
  appliedAt: string;
  updatedAt: string;
}

/** Raw shape of GET .../interviews, /placements/my-interviews — InterviewScheduleResponse */
export interface InterviewSchedule {
  id: string;
  applicationId: string;
  placementId: string;
  companyName: string;
  role: string;
  studentId: string;
  studentName: string;
  roundNumber: number;
  roundName: string;
  scheduledAt: string;
  mode: InterviewMode;
  venueOrLink?: string;
  interviewerName?: string;
  status: InterviewStatus;
  result: InterviewResult;
  feedback?: string;
  createdAt: string;
}

/** Raw shape of GET /placements/dashboard — PlacementDashboardResponse */
export interface PlacementDashboard {
  totalCompanies: number;
  activeCompanies: number;
  totalDrives: number;
  openDrives: number;
  totalApplications: number;
  selectedCount: number;
  shortlistedCount: number;
  rejectedCount: number;
  placementRate: number;
  upcomingInterviewsCount: number;
  applicationsByStatus: Record<string, number>;
  upcomingDrives: PlacementDrive[];
  upcomingInterviews: InterviewSchedule[];
}

export type ResourceType = "DOCUMENT" | "PDF" | "PRESENTATION" | "SPREADSHEET" | "LINK" | "ARCHIVE" | "OTHER";

/** Raw shape of GET/POST /learning-resources — com.rrtechnosoft.lms.dto.response.LearningResourceResponse */
export interface LearningResource {
  id: string;
  title: string;
  description?: string;
  resourceType: ResourceType;
  category?: string;
  courseId?: string;
  fileUrl?: string;
  fileSizeBytes?: number;
  externalUrl?: string;
  isPublished: boolean;
  downloadCount: number;
  uploadedBy: string;
  createdAt: string;
  updatedAt: string;
}

export type VideoSource = "UPLOAD" | "YOUTUBE" | "EXTERNAL";

/** Raw shape of GET/POST /videos — com.rrtechnosoft.lms.dto.response.VideoResourceResponse */
export interface VideoResource {
  id: string;
  title: string;
  description?: string;
  category?: string;
  courseId?: string;
  source: VideoSource;
  videoUrl: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  fileSizeBytes?: number;
  isPublished: boolean;
  viewCount: number;
  uploadedBy: string;
  createdAt: string;
  updatedAt: string;
}
export type ProblemDifficulty = "EASY" | "MEDIUM" | "HARD";

export interface PracticeProblem {
  id: string;
  title: string;
  difficulty: ProblemDifficulty;
  topic: string;
  solvedByMe: boolean;
  successRate: number;
  description: string;
}

export interface AdminDashboardStats {
  totalStudents: number;
  totalCourses: number;
  activeEnrollments: number;
  totalRevenue: number;
  upcomingLiveClasses: number;
  pendingAssignments: number;
  studentGrowth: { month: string; students: number }[];
}

export interface StudentDashboardStats {
  enrolledCourses: number;
  completedCourses: number;
  overallProgress: number;
  attendancePercentage: number;
  pendingAssignments: number;
  upcomingClasses: LiveClass[];
  certificatesEarned: number;
}

// ---------------------------------------------------------------------
// Administration module — matches com.rrtechnosoft.lms.dto.response.*
// under PermissionController / SystemSettingController /
// OrganizationProfileController / MasterDataController /
// FeatureToggleController / NotificationSettingsController /
// SecuritySettingsController / BackupController.
// ---------------------------------------------------------------------

export interface Permission {
  id: string;
  code: string;
  name: string;
  description?: string;
  category: string;
  isSystem: boolean;
  createdAt: string;
}

export interface PermissionMatrixEntry {
  permissionId: string;
  permissionCode: string;
  permissionName: string;
  category: string;
  role: Role;
  allowed: boolean;
}

export interface PermissionMatrix {
  permissions: Permission[];
  entries: PermissionMatrixEntry[];
}

export type SettingValueType = "STRING" | "NUMBER" | "BOOLEAN" | "JSON";
export type SettingCategory = "GENERAL" | "ACADEMICS" | "ENGAGEMENT" | "SECURITY" | "INTEGRATIONS";

export interface SystemSetting {
  id: string;
  key: string;
  value: string | null;
  valueType: SettingValueType;
  category: SettingCategory;
  description?: string;
  isEditable: boolean;
  updatedAt: string;
}

export interface OrganizationProfile {
  id: string;
  orgName: string;
  legalName?: string;
  logoUrl?: string;
  faviconUrl?: string;
  website?: string;
  supportEmail?: string;
  supportPhone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  taxId?: string;
  timezone: string;
  dateFormat: string;
  updatedAt: string;
}

export interface MasterDataCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  isSystem: boolean;
  updatedAt: string;
}

export interface MasterDataItem {
  id: string;
  categoryId: string;
  categoryCode: string;
  code: string;
  label: string;
  description?: string;
  sortOrder: number;
  isActive: boolean;
  metadata?: string;
  updatedAt: string;
}

export interface FeatureToggle {
  id: string;
  featureKey: string;
  name: string;
  description?: string;
  enabled: boolean;
  updatedAt: string;
}

export type DigestFrequency = "INSTANT" | "DAILY" | "WEEKLY" | "NONE";

export interface NotificationSettings {
  id: string;
  smtpHost?: string;
  smtpPort: number;
  smtpUsername?: string;
  smtpConfigured: boolean;
  smtpUseTls: boolean;
  fromName: string;
  fromEmail?: string;
  emailNotificationsEnabled: boolean;
  smsNotificationsEnabled: boolean;
  pushNotificationsEnabled: boolean;
  digestFrequency: DigestFrequency;
  updatedAt: string;
}

export interface SecuritySettings {
  id: string;
  passwordMinLength: number;
  passwordRequireUppercase: boolean;
  passwordRequireNumber: boolean;
  passwordRequireSpecialChar: boolean;
  passwordExpiryDays: number;
  maxLoginAttempts: number;
  lockoutDurationMinutes: number;
  sessionTimeoutMinutes: number;
  mfaRequiredForAdmins: boolean;
  allowedIpRanges?: string;
  forceLogoutOnPasswordChange: boolean;
  updatedAt: string;
}

export type BackupStorageType = "LOCAL" | "S3";
export type BackupRunStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED";

export interface BackupConfig {
  id: string;
  scheduleCron: string;
  retentionDays: number;
  storageType: BackupStorageType;
  storageLocation: string;
  autoBackupEnabled: boolean;
  updatedAt: string;
}

export interface BackupRun {
  id: string;
  status: BackupRunStatus;
  triggeredBy?: string;
  fileLocation?: string;
  sizeMb?: number;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}

export type NotificationType = "ANNOUNCEMENT" | "ASSIGNMENT" | "LIVE_CLASS" | "PLACEMENT" | "CERTIFICATE" | "SYSTEM" | "TASK";

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  body?: string;
  link?: string;
  read: boolean;
  createdAt: string;
}

// ---------------------------------------------------------------------------
// Reports & Analytics module — matches com.rrtechnosoft.lms.dto.response.reports.*
// ---------------------------------------------------------------------------

export interface TrendPoint {
  label: string;
  value: number;
}

export interface CourseDistribution {
  category: string;
  courseCount: number;
  enrollmentCount: number;
}

export interface DashboardAnalytics {
  totalStudents: number;
  totalCourses: number;
  activeEnrollments: number;
  totalRevenue: number;
  upcomingLiveClasses: number;
  pendingAssignments: number;
  studentGrowth: TrendPoint[];
  totalFaculty: number;
  avgAttendancePercentage: number;
  avgCourseCompletionPercentage: number;
  averageRevenuePerStudent: number;
  revenueTrend: TrendPoint[];
  attendanceTrend: TrendPoint[];
  courseDistribution: CourseDistribution[];
}

export interface StudentReportRow {
  studentId: string;
  studentCode: string | null;
  fullName: string;
  email: string | null;
  batch: string | null;
  branch: string | null;
  college: string | null;
  coursesEnrolled: number;
  avgProgressPercentage: number;
  attendancePercentage: number;
  avgAssignmentScore: number;
  assignmentsSubmitted: number;
  assignmentsPending: number;
}

export interface FacultyReportRow {
  instructorName: string;
  coursesHandled: number;
  totalStudents: number;
  avgCourseRating: number;
  avgCompletionPercentage: number;
  revenueGenerated: number;
}

export interface AttendanceReportRow {
  courseId: string;
  courseTitle: string;
  sessionsHeld: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  attendancePercentage: number;
}

export interface AssignmentReportRow {
  assignmentId: string;
  assignmentTitle: string;
  courseTitle: string | null;
  dueAt: string | null;
  totalStudents: number;
  submittedCount: number;
  gradedCount: number;
  lateCount: number;
  pendingCount: number;
  avgScore: number;
  submissionRatePercentage: number;
}

export interface RevenueReportRow {
  courseId: string;
  courseTitle: string;
  category: string | null;
  unitPrice: number;
  paidEnrollments: number;
  droppedOrPendingEnrollments: number;
  totalRevenue: number;
}

export interface RevenueSummary {
  totalRevenue: number;
  totalPaidEnrollments: number;
  averageOrderValue: number;
  monthlyTrend: TrendPoint[];
}

/* ------------------------------------------------------------------ */
/* Finance module — matches com.rrtechnosoft.lms.dto.response / entity.enums */
/* ------------------------------------------------------------------ */

export type FeeStatus = "PENDING" | "PARTIAL" | "PAID" | "OVERDUE" | "WAIVED" | "CANCELLED";
export type InstallmentStatus = "PENDING" | "PARTIAL" | "PAID" | "OVERDUE" | "WAIVED";
export type PaymentStatus =
  | "INITIATED"
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED"
  | "PARTIALLY_REFUNDED";
export type PaymentMethod = "CARD" | "UPI" | "NETBANKING" | "WALLET" | "CASH" | "BANK_TRANSFER" | "CHEQUE";
export type PaymentGatewayProvider = "RAZORPAY" | "MANUAL";
export type RefundStatus = "REQUESTED" | "APPROVED" | "REJECTED" | "PROCESSED" | "FAILED";
export type DiscountType = "PERCENTAGE" | "FLAT";
export type FineStatus = "PENDING" | "WAIVED" | "PAID";

/** com.rrtechnosoft.lms.dto.response.FeeStructureInstallmentResponse */
export interface FeeStructureInstallment {
  id: string;
  installmentNumber: number;
  amount: number;
  dueAfterDays: number;
}

/** com.rrtechnosoft.lms.dto.response.FeeStructureResponse */
export interface FeeStructure {
  id: string;
  courseId: string | null;
  courseTitle: string | null;
  name: string;
  description: string | null;
  totalAmount: number;
  currency: string;
  installmentCount: number;
  isActive: boolean;
  installments: FeeStructureInstallment[];
  createdAt: string;
}

/** com.rrtechnosoft.lms.dto.response.StudentFeeInstallmentResponse */
export interface StudentFeeInstallment {
  id: string;
  installmentNumber: number;
  amount: number;
  dueDate: string;
  paidAmount: number;
  balanceDue: number;
  status: InstallmentStatus;
  paidAt: string | null;
}

/** com.rrtechnosoft.lms.dto.response.StudentFeeResponse */
export interface StudentFee {
  id: string;
  studentId: string;
  studentName: string;
  studentIdNumber: string | null;
  courseId: string | null;
  courseTitle: string | null;
  feeStructureId: string | null;
  feeStructureName: string | null;
  totalAmount: number;
  discountAmount: number;
  fineAmount: number;
  netPayable: number;
  amountPaid: number;
  balanceDue: number;
  currency: string;
  status: FeeStatus;
  installments: StudentFeeInstallment[];
  createdAt: string;
}

/** com.rrtechnosoft.lms.dto.response.PaymentResponse */
export interface Payment {
  id: string;
  studentFeeId: string;
  installmentId: string | null;
  studentId: string;
  studentName: string;
  amount: number;
  currency: string;
  method: PaymentMethod | null;
  gatewayProvider: PaymentGatewayProvider | null;
  gatewayOrderId: string | null;
  gatewayPaymentId: string | null;
  status: PaymentStatus;
  failureReason: string | null;
  refundedAmount: number;
  paidAt: string | null;
  createdAt: string;
}

/** com.rrtechnosoft.lms.dto.response.PaymentOrderResponse — used to open the gateway checkout widget. */
export interface PaymentOrder {
  paymentId: string;
  gatewayOrderId: string;
  amount: number;
  currency: string;
  keyId: string;
  provider: string;
}

/** com.rrtechnosoft.lms.dto.response.ReceiptResponse */
export interface Receipt {
  id: string;
  paymentId: string;
  studentFeeId: string;
  receiptNumber: string;
  amount: number;
  issuedAt: string;
}

/** com.rrtechnosoft.lms.dto.response.RefundResponse */
export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  reason: string;
  status: RefundStatus;
  gatewayRefundId: string | null;
  requestedAt: string;
  processedAt: string | null;
}

/** com.rrtechnosoft.lms.dto.response.FeeSummaryReportResponse */
export interface FeeSummaryReport {
  totalBilled: number;
  totalCollected: number;
  totalOutstanding: number;
  totalStudentFees: number;
  overdueCount: number;
}

/** com.rrtechnosoft.lms.dto.response.AuditLogResponse */
export interface AuditLogEntry {
  id: string;
  actorId: string | null;
  actorName: string;
  actorEmail: string | null;
  action: string;
  entityType: string | null;
  entityId: string | null;
  metadata: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AuditLogSearchParams {
  actorId?: string;
  action?: string;
  entityType?: string;
  entityId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface ChatConversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
}

export interface SendChatMessageResult {
  conversationId: string;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
}
