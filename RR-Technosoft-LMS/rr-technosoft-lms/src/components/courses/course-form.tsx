"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { courseSchema, type CourseInput } from "@/lib/validations/course";
import { coursesApi } from "@/lib/api/courses";
import { extractErrorMessage } from "@/lib/api/client";
import { Course } from "@/types";

interface CourseFormProps {
  course?: Course;
}

export function CourseForm({ course }: CourseFormProps) {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<CourseInput>({
    resolver: zodResolver(courseSchema),
    defaultValues: course
      ? {
          title: course.title,
          description: course.description,
          category: course.category,
          level: course.level,
          durationWeeks: course.durationWeeks,
          price: course.price,
          instructorName: course.instructorName,
        }
      : { level: "BEGINNER" },
  });

  async function onSubmit(values: CourseInput) {
    setSubmitting(true);
    try {
      if (course) {
        await coursesApi.update(course.id, values);
        toast.success("Course updated");
      } else {
        const created = await coursesApi.create(values);
        toast.success("Course created");
        router.push(`/admin/courses/${created.id}`);
        return;
      }
      router.push("/admin/courses");
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card>
      <CardContent className="p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="title">Course title</Label>
              <Input id="title" placeholder="Full Stack Web Development" {...register("title")} />
              {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
            </div>

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" rows={4} placeholder="What will students learn in this course?" {...register("description")} />
              {errors.description && <p className="text-xs text-destructive">{errors.description.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="category">Category</Label>
              <Input id="category" placeholder="Web Development" {...register("category")} />
              {errors.category && <p className="text-xs text-destructive">{errors.category.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label>Level</Label>
              <Controller
                control={control}
                name="level"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="BEGINNER">Beginner</SelectItem>
                      <SelectItem value="INTERMEDIATE">Intermediate</SelectItem>
                      <SelectItem value="ADVANCED">Advanced</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="instructorName">Instructor</Label>
              <Input id="instructorName" placeholder="Instructor name" {...register("instructorName")} />
              {errors.instructorName && <p className="text-xs text-destructive">{errors.instructorName.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="durationWeeks">Duration (weeks)</Label>
              <Input id="durationWeeks" type="number" min={1} {...register("durationWeeks")} />
              {errors.durationWeeks && <p className="text-xs text-destructive">{errors.durationWeeks.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="price">Price (₹)</Label>
              <Input id="price" type="number" min={0} {...register("price")} />
              {errors.price && <p className="text-xs text-destructive">{errors.price.message}</p>}
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t pt-5">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {course ? "Save changes" : "Create course"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
