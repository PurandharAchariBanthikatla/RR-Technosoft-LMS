import { z } from "zod";

/**
 * identifier = email (SUPER_ADMIN/ADMIN) or Student ID like "RRT2026S0001" (STUDENT).
 * The backend detects which by checking for "@" — the frontend doesn't need to know.
 */
export const loginSchema = z.object({
  identifier: z.string().min(1, "Enter your email or Student ID"),
  password: z.string().min(1, "Password is required"),
});
export type LoginInput = z.infer<typeof loginSchema>;

// There is no public self-registration endpoint on the backend. Students and
// admins are created by an authorized admin — see the schemas below, used by
// the "Add student" / "Add admin" management dialogs.

export const createStudentSchema = z.object({
  fullName: z.string().min(2, "Name must be at least 2 characters"),
  phone: z.string().min(10, "Enter a valid phone number").optional().or(z.literal("")),
  initialPassword: z.string().min(8, "Password must be at least 8 characters"),
  batch: z.string().optional().or(z.literal("")),
  branch: z.string().optional().or(z.literal("")),
  college: z.string().optional().or(z.literal("")),
  graduationYear: z.coerce.number().int().optional(),
});
export type CreateStudentInput = z.infer<typeof createStudentSchema>;

// Same fields as createStudentSchema minus initialPassword (edit doesn't
// touch the account password — that's a separate reset-password flow).
export const updateStudentSchema = createStudentSchema.omit({ initialPassword: true });
export type UpdateStudentInput = z.infer<typeof updateStudentSchema>;

export const createAdminSchema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  fullName: z.string().min(2, "Name must be at least 2 characters"),
  phone: z.string().min(10, "Enter a valid phone number").optional().or(z.literal("")),
  department: z.string().optional().or(z.literal("")),
  designation: z.string().optional().or(z.literal("")),
});
export type CreateAdminInput = z.infer<typeof createAdminSchema>;
