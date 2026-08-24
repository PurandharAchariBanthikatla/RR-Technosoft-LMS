import { z } from "zod";

export const videoResourceSchema = z.object({
  title: z.string().min(3, "Title must be at least 3 characters"),
  description: z.string().optional(),
  category: z.string().optional(),
  courseId: z.string().optional(),
  source: z.enum(["UPLOAD", "YOUTUBE", "EXTERNAL"]),
  videoUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  thumbnailUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
  durationSeconds: z.coerce.number().min(0).optional(),
});
export type VideoResourceInputForm = z.infer<typeof videoResourceSchema>;
