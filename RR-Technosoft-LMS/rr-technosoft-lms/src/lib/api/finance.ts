import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import {
  FeeStructure,
  StudentFee,
  Payment,
  PaymentOrder,
  Receipt,
  Refund,
  FeeSummaryReport,
  Paginated,
  FeeStatus,
  PaymentStatus,
  DiscountType,
  PaymentMethod,
} from "@/types";

export interface FeeInstallmentInput {
  installmentNumber: number;
  amount: number;
  dueAfterDays: number;
}

export interface CreateFeeStructurePayload {
  courseId?: string;
  name: string;
  description?: string;
  totalAmount: number;
  currency?: string;
  installments: FeeInstallmentInput[];
}

export interface UpdateFeeStructurePayload {
  name?: string;
  description?: string;
  isActive?: boolean;
}

/** Matches com.rrtechnosoft.lms.controller.FeeStructureController (/finance/fee-structures). */
export const feeStructuresApi = {
  list: (params?: { courseId?: string; activeOnly?: boolean; page?: number; size?: number }) =>
    apiClient.get<Paginated<FeeStructure>>(API_ROUTES.feeStructures, { params }).then((r) => r.data),

  get: (id: string) => apiClient.get<FeeStructure>(`${API_ROUTES.feeStructures}/${id}`).then((r) => r.data),

  create: (payload: CreateFeeStructurePayload) =>
    apiClient.post<FeeStructure>(API_ROUTES.feeStructures, payload).then((r) => r.data),

  update: (id: string, payload: UpdateFeeStructurePayload) =>
    apiClient.patch<FeeStructure>(`${API_ROUTES.feeStructures}/${id}`, payload).then((r) => r.data),

  deactivate: (id: string) => apiClient.delete(`${API_ROUTES.feeStructures}/${id}`),
};

export interface AssignFeeStructurePayload {
  studentId: string;
  courseId?: string;
  feeStructureId?: string;
  totalAmount?: number;
  startDate: string; // ISO date (yyyy-MM-dd)
  installmentOverrides?: FeeInstallmentInput[];
}

export interface CreateDiscountPayload {
  type: DiscountType;
  value: number;
  reason: string;
}

export interface CreateFinePayload {
  installmentId?: string;
  amount: number;
  reason: string;
}

/** Matches com.rrtechnosoft.lms.controller.StudentFeeController (/finance/student-fees). */
export const studentFeesApi = {
  list: (params?: { studentId?: string; courseId?: string; status?: FeeStatus; page?: number; size?: number }) =>
    apiClient.get<Paginated<StudentFee>>(API_ROUTES.studentFees, { params }).then((r) => r.data),

  mine: () => apiClient.get<StudentFee[]>(`${API_ROUTES.studentFees}/me`).then((r) => r.data),

  get: (id: string) => apiClient.get<StudentFee>(`${API_ROUTES.studentFees}/${id}`).then((r) => r.data),

  invoicePdfUrl: (id: string) => `${API_ROUTES.studentFees}/${id}/invoice`,

  assign: (payload: AssignFeeStructurePayload) =>
    apiClient.post<StudentFee>(API_ROUTES.studentFees, payload).then((r) => r.data),

  addDiscount: (id: string, payload: CreateDiscountPayload) =>
    apiClient.post<StudentFee>(`${API_ROUTES.studentFees}/${id}/discounts`, payload).then((r) => r.data),

  addFine: (id: string, payload: CreateFinePayload) =>
    apiClient.post<StudentFee>(`${API_ROUTES.studentFees}/${id}/fines`, payload).then((r) => r.data),

  waiveFine: (id: string, fineId: string) =>
    apiClient.patch<StudentFee>(`${API_ROUTES.studentFees}/${id}/fines/${fineId}/waive`).then((r) => r.data),
};

export interface InitiatePaymentPayload {
  studentFeeId: string;
  installmentId?: string;
  amount: number;
}

export interface VerifyPaymentPayload {
  paymentId: string;
  gatewayOrderId: string;
  gatewayPaymentId: string;
  gatewaySignature: string;
}

export interface RecordManualPaymentPayload {
  studentFeeId: string;
  installmentId?: string;
  amount: number;
  method: PaymentMethod;
  note?: string;
}

export interface RefundPayload {
  amount: number;
  reason: string;
}

/** Matches com.rrtechnosoft.lms.controller.PaymentController (/finance/payments). */
export const paymentsApi = {
  list: (params?: { studentId?: string; studentFeeId?: string; status?: PaymentStatus; page?: number; size?: number }) =>
    apiClient.get<Paginated<Payment>>(API_ROUTES.payments, { params }).then((r) => r.data),

  mine: () => apiClient.get<Payment[]>(`${API_ROUTES.payments}/me`).then((r) => r.data),

  get: (id: string) => apiClient.get<Payment>(`${API_ROUTES.payments}/${id}`).then((r) => r.data),

  initiate: (payload: InitiatePaymentPayload) =>
    apiClient.post<PaymentOrder>(`${API_ROUTES.payments}/initiate`, payload).then((r) => r.data),

  verify: (payload: VerifyPaymentPayload) =>
    apiClient.post<Payment>(`${API_ROUTES.payments}/verify`, payload).then((r) => r.data),

  recordManual: (payload: RecordManualPaymentPayload) =>
    apiClient.post<Payment>(`${API_ROUTES.payments}/manual`, payload).then((r) => r.data),

  refund: (id: string, payload: RefundPayload) =>
    apiClient.post<Refund>(`${API_ROUTES.payments}/${id}/refund`, payload).then((r) => r.data),
};

/** Matches com.rrtechnosoft.lms.controller.ReceiptController (/finance/receipts). */
export const receiptsApi = {
  list: (params?: { studentId?: string; page?: number; size?: number }) =>
    apiClient.get<Paginated<Receipt>>(API_ROUTES.receipts, { params }).then((r) => r.data),

  mine: () => apiClient.get<Receipt[]>(`${API_ROUTES.receipts}/me`).then((r) => r.data),

  get: (id: string) => apiClient.get<Receipt>(`${API_ROUTES.receipts}/${id}`).then((r) => r.data),

  pdfUrl: (id: string) => `${API_ROUTES.receipts}/${id}/pdf`,
};

/** Matches com.rrtechnosoft.lms.controller.FinanceReportController (/finance/reports). */
export const financeReportsApi = {
  summary: (courseId?: string) =>
    apiClient.get<FeeSummaryReport>(`${API_ROUTES.financeReports}/summary`, { params: { courseId } }).then((r) => r.data),

  studentFees: (params?: { courseId?: string; status?: FeeStatus; page?: number; size?: number }) =>
    apiClient
      .get<Paginated<StudentFee>>(`${API_ROUTES.financeReports}/student-fees`, { params })
      .then((r) => r.data),
};

/**
 * Fetches a protected PDF (invoice/receipt) as a blob using the authenticated
 * apiClient — a plain <a href> can't attach the Bearer token — and opens it
 * in a new tab. Falls back to a same-tab navigation if popups are blocked.
 */
export async function openPdfInNewTab(url: string, filename: string) {
  const response = await apiClient.get(url, { responseType: "blob" });
  const blobUrl = URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
  const win = window.open(blobUrl, "_blank");
  if (!win) {
    const a = document.createElement("a");
    a.href = blobUrl;
    a.download = filename;
    a.click();
  }
  setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
}
