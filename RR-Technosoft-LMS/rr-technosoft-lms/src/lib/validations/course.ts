import { z } from "zod";

export const courseSchema = z.object({
  title: z.string().min(3, "Title must be at least 3 characters"),
  description: z.string().min(20, "Description should be at least 20 characters"),
  category: z.string().min(1, "Select a category"),
  level: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]),
  durationWeeks: z.coerce.number().min(1, "Duration must be at least 1 week"),
  price: z.coerce.number().min(0, "Price cannot be negative"),
  instructorName: z.string().min(2, "Instructor name is required"),
});
export type CourseInput = z.infer<typeof courseSchema>;
