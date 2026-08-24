import { z } from "zod";

export const learningResourceSchema = z.object({
  title: z.string().min(3, "Title must be at least 3 characters"),
  description: z.string().optional(),
  resourceType: z.enum(["DOCUMENT", "PDF", "PRESENTATION", "SPREADSHEET", "LINK", "ARCHIVE", "OTHER"]),
  category: z.string().optional(),
  courseId: z.string().optional(),
  externalUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
});
export type LearningResourceInputForm = z.infer<typeof learningResourceSchema>;
