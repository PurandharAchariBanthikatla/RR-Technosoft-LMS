import { z } from "zod";

export const placementSchema = z
  .object({
    companyId: z.string().optional(),
    companyName: z.string().optional(),
    companyLogoUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
    role: z.string().min(2, "Role is required"),
    description: z.string().optional(),
    eligibility: z.string().optional(),
    skillsRequired: z.string().optional(), // comma-separated in the form, split before submit
    allowedBranches: z.string().optional(),
    salaryMin: z.coerce.number().min(0).optional(),
    salaryMax: z.coerce.number().min(0).optional(),
    minCgpa: z.coerce.number().min(0).max(10).optional(),
    location: z.string().optional(),
    jobType: z.enum(["FULL_TIME", "INTERNSHIP", "PART_TIME", "CONTRACT"]),
    driveDate: z.string().optional(),
    lastDateToApply: z.string().min(1, "Application deadline is required"),
    applicationLink: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  })
  .refine((data) => data.companyId || (data.companyName && data.companyName.length > 1), {
    message: "Select a company or enter a company name",
    path: ["companyName"],
  });
export type PlacementInput = z.infer<typeof placementSchema>;
