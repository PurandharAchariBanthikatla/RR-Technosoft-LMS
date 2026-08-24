import { z } from "zod";

export const companySchema = z.object({
  name: z.string().min(2, "Company name is required"),
  logoUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  website: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  industry: z.string().optional(),
  description: z.string().optional(),
  contactPersonName: z.string().optional(),
  contactEmail: z.string().email("Enter a valid email").optional().or(z.literal("")),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
});
export type CompanyInput = z.infer<typeof companySchema>;
