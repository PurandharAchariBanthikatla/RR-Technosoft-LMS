import { z } from "zod";

export const organizationProfileSchema = z.object({
  orgName: z.string().min(2, "Organization name is required"),
  legalName: z.string().optional().or(z.literal("")),
  logoUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  faviconUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  website: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  supportEmail: z.string().email("Enter a valid email").optional().or(z.literal("")),
  supportPhone: z.string().optional().or(z.literal("")),
  addressLine1: z.string().optional().or(z.literal("")),
  addressLine2: z.string().optional().or(z.literal("")),
  city: z.string().optional().or(z.literal("")),
  state: z.string().optional().or(z.literal("")),
  country: z.string().optional().or(z.literal("")),
  postalCode: z.string().optional().or(z.literal("")),
  taxId: z.string().optional().or(z.literal("")),
  timezone: z.string().min(1, "Timezone is required"),
  dateFormat: z.string().min(1, "Date format is required"),
});
export type OrganizationProfileInput = z.infer<typeof organizationProfileSchema>;

export const masterDataCategorySchema = z.object({
  code: z
    .string()
    .min(2, "Code is required")
    .regex(/^[A-Z0-9_]+$/, "Upper-case letters, digits and underscores only"),
  name: z.string().min(2, "Name is required"),
  description: z.string().optional().or(z.literal("")),
});
export type MasterDataCategoryInput = z.infer<typeof masterDataCategorySchema>;

export const masterDataItemSchema = z.object({
  code: z.string().min(1, "Code is required"),
  label: z.string().min(1, "Label is required"),
  description: z.string().optional().or(z.literal("")),
  sortOrder: z.coerce.number().int().optional(),
});
export type MasterDataItemInput = z.infer<typeof masterDataItemSchema>;

export const securitySettingsSchema = z.object({
  passwordMinLength: z.coerce.number().int().min(6, "At least 6 characters"),
  passwordRequireUppercase: z.boolean(),
  passwordRequireNumber: z.boolean(),
  passwordRequireSpecialChar: z.boolean(),
  passwordExpiryDays: z.coerce.number().int().min(0),
  maxLoginAttempts: z.coerce.number().int().min(1),
  lockoutDurationMinutes: z.coerce.number().int().min(1),
  sessionTimeoutMinutes: z.coerce.number().int().min(1),
  mfaRequiredForAdmins: z.boolean(),
  allowedIpRanges: z.string().optional().or(z.literal("")),
  forceLogoutOnPasswordChange: z.boolean(),
});
export type SecuritySettingsInput = z.infer<typeof securitySettingsSchema>;

export const notificationSettingsSchema = z.object({
  smtpHost: z.string().optional().or(z.literal("")),
  smtpPort: z.coerce.number().int().min(1).max(65535),
  smtpUsername: z.string().optional().or(z.literal("")),
  smtpPassword: z.string().optional().or(z.literal("")),
  smtpUseTls: z.boolean(),
  fromName: z.string().min(1, "Sender name is required"),
  fromEmail: z.string().email("Enter a valid email").optional().or(z.literal("")),
  emailNotificationsEnabled: z.boolean(),
  smsNotificationsEnabled: z.boolean(),
  pushNotificationsEnabled: z.boolean(),
  digestFrequency: z.enum(["INSTANT", "DAILY", "WEEKLY", "NONE"]),
});
export type NotificationSettingsInput = z.infer<typeof notificationSettingsSchema>;

export const backupConfigSchema = z.object({
  scheduleCron: z.string().min(1, "Cron expression is required"),
  retentionDays: z.coerce.number().int().min(1),
  storageType: z.enum(["LOCAL", "S3"]),
  storageLocation: z.string().min(1, "Storage location is required"),
  autoBackupEnabled: z.boolean(),
});
export type BackupConfigInput = z.infer<typeof backupConfigSchema>;

export const systemSettingSchema = z.object({
  key: z.string().min(2, "Key is required"),
  value: z.string().optional().or(z.literal("")),
  valueType: z.enum(["STRING", "NUMBER", "BOOLEAN", "JSON"]),
  category: z.enum(["GENERAL", "ACADEMICS", "ENGAGEMENT", "SECURITY", "INTEGRATIONS"]),
  description: z.string().optional().or(z.literal("")),
});
export type SystemSettingInput = z.infer<typeof systemSettingSchema>;
