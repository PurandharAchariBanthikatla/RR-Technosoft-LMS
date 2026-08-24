import { apiClient } from "./client";
import { API_ROUTES, Role } from "@/lib/constants";
import {
  AuditLogEntry,
  AuditLogSearchParams,
  BackupConfig,
  BackupRun,
  FeatureToggle,
  MasterDataCategory,
  MasterDataItem,
  NotificationSettings,
  OrganizationProfile,
  Paginated,
  PermissionMatrix,
  SecuritySettings,
  SettingCategory,
  SystemSetting,
} from "@/types";

// ----- Permission Matrix (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.PermissionController (/administration/permissions).
export const permissionsApi = {
  getMatrix: () => apiClient.get<PermissionMatrix>(API_ROUTES.permissionMatrix).then((r) => r.data),

  updateMatrix: (entries: { permissionId: string; role: Role; allowed: boolean }[]) =>
    apiClient.put<PermissionMatrix>(API_ROUTES.permissionMatrix, { entries }).then((r) => r.data),
};

// ----- System Settings (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.SystemSettingController (/system-settings).
export const systemSettingsApi = {
  list: (category?: SettingCategory) =>
    apiClient
      .get<SystemSetting[]>(API_ROUTES.systemSettings, { params: category ? { category } : undefined })
      .then((r) => r.data),

  create: (payload: { key: string; value?: string; valueType: string; category: SettingCategory; description?: string }) =>
    apiClient.post<SystemSetting>(API_ROUTES.systemSettings, payload).then((r) => r.data),

  updateValue: (id: string, value: string) =>
    apiClient.patch<SystemSetting>(`${API_ROUTES.systemSettings}/${id}`, { value }).then((r) => r.data),

  remove: (id: string) => apiClient.delete(`${API_ROUTES.systemSettings}/${id}`),
};

// ----- Organization Profile (GET public, PUT SUPER_ADMIN) -----
// Matches com.rrtechnosoft.lms.controller.OrganizationProfileController.
export const organizationProfileApi = {
  get: () => apiClient.get<OrganizationProfile>(API_ROUTES.organizationProfile).then((r) => r.data),

  update: (payload: Omit<OrganizationProfile, "id" | "updatedAt">) =>
    apiClient.put<OrganizationProfile>(API_ROUTES.organizationProfile, payload).then((r) => r.data),
};

// ----- Master Data (GET open to authenticated users, writes SUPER_ADMIN) -----
// Matches com.rrtechnosoft.lms.controller.MasterDataController.
export const masterDataApi = {
  listCategories: () =>
    apiClient.get<MasterDataCategory[]>(API_ROUTES.masterDataCategories).then((r) => r.data),

  createCategory: (payload: { code: string; name: string; description?: string }) =>
    apiClient.post<MasterDataCategory>(API_ROUTES.masterDataCategories, payload).then((r) => r.data),

  updateCategory: (id: string, payload: { name: string; description?: string }) =>
    apiClient.patch<MasterDataCategory>(`${API_ROUTES.masterDataCategories}/${id}`, payload).then((r) => r.data),

  removeCategory: (id: string) => apiClient.delete(`${API_ROUTES.masterDataCategories}/${id}`),

  listItems: (categoryId: string, activeOnly = false) =>
    apiClient
      .get<MasterDataItem[]>(API_ROUTES.masterDataItems(categoryId), { params: { activeOnly } })
      .then((r) => r.data),

  createItem: (categoryId: string, payload: { code: string; label: string; description?: string; sortOrder?: number }) =>
    apiClient.post<MasterDataItem>(API_ROUTES.masterDataItems(categoryId), payload).then((r) => r.data),

  updateItem: (
    itemId: string,
    payload: { label: string; description?: string; sortOrder?: number; isActive?: boolean }
  ) => apiClient.patch<MasterDataItem>(API_ROUTES.masterDataItem(itemId), payload).then((r) => r.data),

  removeItem: (itemId: string) => apiClient.delete(API_ROUTES.masterDataItem(itemId)),
};

// ----- Feature Toggles (GET any authenticated user, PATCH SUPER_ADMIN) -----
// Matches com.rrtechnosoft.lms.controller.FeatureToggleController.
export const featureTogglesApi = {
  list: () => apiClient.get<FeatureToggle[]>(API_ROUTES.featureToggles).then((r) => r.data),

  update: (featureKey: string, enabled: boolean) =>
    apiClient.patch<FeatureToggle>(`${API_ROUTES.featureToggles}/${featureKey}`, { enabled }).then((r) => r.data),
};

// ----- Notification / Email Settings (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.NotificationSettingsController.
export const notificationSettingsApi = {
  get: () => apiClient.get<NotificationSettings>(API_ROUTES.notificationSettings).then((r) => r.data),

  update: (payload: Omit<NotificationSettings, "id" | "smtpConfigured" | "updatedAt"> & { smtpPassword?: string }) =>
    apiClient.put<NotificationSettings>(API_ROUTES.notificationSettings, payload).then((r) => r.data),
};

// ----- Security Settings (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.SecuritySettingsController.
export const securitySettingsApi = {
  get: () => apiClient.get<SecuritySettings>(API_ROUTES.securitySettings).then((r) => r.data),

  update: (payload: Omit<SecuritySettings, "id" | "updatedAt">) =>
    apiClient.put<SecuritySettings>(API_ROUTES.securitySettings, payload).then((r) => r.data),
};

// ----- Backup & Restore (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.BackupController.
export const backupApi = {
  getConfig: () => apiClient.get<BackupConfig>(API_ROUTES.backupConfig).then((r) => r.data),

  updateConfig: (payload: Omit<BackupConfig, "id" | "updatedAt">) =>
    apiClient.put<BackupConfig>(API_ROUTES.backupConfig, payload).then((r) => r.data),

  listRuns: (params?: { page?: number; size?: number }) =>
    apiClient.get<Paginated<BackupRun>>(API_ROUTES.backupRuns, { params }).then((r) => r.data),

  trigger: () => apiClient.post<BackupRun>(API_ROUTES.backupRuns).then((r) => r.data),
};

// ----- Audit Logs (SUPER_ADMIN only) -----
// Matches com.rrtechnosoft.lms.controller.AuditLogController. Every param is
// optional — the backend applies only the filters that are present.
export const auditLogsApi = {
  search: (params?: AuditLogSearchParams) =>
    apiClient.get<Paginated<AuditLogEntry>>(API_ROUTES.auditLogs, { params }).then((r) => r.data),
};
