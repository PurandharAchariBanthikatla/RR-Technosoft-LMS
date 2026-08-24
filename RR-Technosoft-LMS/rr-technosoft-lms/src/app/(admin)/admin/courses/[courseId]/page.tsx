"use client";

import { useParams, useSearchParams, useRouter } from "next/navigation";
import { useState } from "react";
import { Plus, Users, Clock, IndianRupee, Star, Pencil, CheckCircle2, Archive, RotateCcw } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { coursesApi } from "@/lib/api/courses";
import { extractErrorMessage } from "@/lib/api/client";
import { CourseForm } from "@/components/courses/course-form";
import { CreateModuleDialog } from "@/components/courses/create-module-dialog";
import { Layers } from "lucide-react";

export default function AdminCourseDetailPage() {
  const params = useParams<{ courseId: string }>();
  const searchParams = useSearchParams();
  const router = useRouter();
  const isEditing = searchParams.get("edit") === "1";

  const [moduleDialogOpen, setModuleDialogOpen] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState(false);

  const courseFetch = useFetch(() => coursesApi.get(params.courseId), [params.courseId]);
  const modulesFetch = useFetch(() => coursesApi.modules(params.courseId), [params.courseId]);

  if (courseFetch.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (courseFetch.error || !courseFetch.data) {
    return <ErrorState message={courseFetch.error ?? "Course not found"} onRetry={courseFetch.refetch} />;
  }

  const course = courseFetch.data;

  if (isEditing) {
    return (
      <div>
        <PageHeader title={`Edit ${course.title}`} description="Update the course catalog details below." />
        <CourseForm course={course} />
      </div>
    );
  }

  async function handleCreateModule(title: string) {
    try {
      await coursesApi.createModule(course.id, { title });
      toast.success(`Module "${title}" added`);
      modulesFetch.refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
      throw err;
    }
  }

  async function handleStatusChange(status: "DRAFT" | "PUBLISHED" | "ARCHIVED") {
    setStatusUpdating(true);
    try {
      await coursesApi.setStatus(course.id, status);
      toast.success(`Course marked as ${status.toLowerCase()}`);
      courseFetch.refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setStatusUpdating(false);
    }
  }

  return (
    <div>
      <PageHeader
        title={course.title}
        description={course.description}
        actions={
          <div className="flex items-center gap-2">
            <StatusBadge status={course.status} />
            <Button variant="outline" size="sm" className="gap-2" onClick={() => router.push(`/admin/courses/${course.id}?edit=1`)}>
              <Pencil className="h-4 w-4" /> Edit
            </Button>
            {course.status !== "PUBLISHED" && (
              <Button size="sm" className="gap-2" disabled={statusUpdating} onClick={() => handleStatusChange("PUBLISHED")}>
                <CheckCircle2 className="h-4 w-4" /> Publish
              </Button>
            )}
            {course.status === "PUBLISHED" && (
              <Button variant="outline" size="sm" className="gap-2" disabled={statusUpdating} onClick={() => handleStatusChange("ARCHIVED")}>
                <Archive className="h-4 w-4" /> Archive
              </Button>
            )}
            {course.status === "ARCHIVED" && (
              <Button variant="outline" size="sm" className="gap-2" disabled={statusUpdating} onClick={() => handleStatusChange("DRAFT")}>
                <RotateCcw className="h-4 w-4" /> Move to draft
              </Button>
            )}
          </div>
        }
      />

      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card><CardContent className="flex items-center gap-3 p-4">
          <Users className="h-4 w-4 text-primary" />
          <div><p className="text-xs text-muted-foreground">Enrolled</p><p className="font-semibold">{course.studentsEnrolled}</p></div>
        </CardContent></Card>
        <Card><CardContent className="flex items-center gap-3 p-4">
          <Clock className="h-4 w-4 text-primary" />
          <div><p className="text-xs text-muted-foreground">Duration</p><p className="font-semibold">{course.durationWeeks} weeks</p></div>
        </CardContent></Card>
        <Card><CardContent className="flex items-center gap-3 p-4">
          <IndianRupee className="h-4 w-4 text-primary" />
          <div><p className="text-xs text-muted-foreground">Price</p><p className="font-semibold">₹{course.price.toLocaleString("en-IN")}</p></div>
        </CardContent></Card>
        <Card><CardContent className="flex items-center gap-3 p-4">
          <Star className="h-4 w-4 text-primary" />
          <div><p className="text-xs text-muted-foreground">Rating</p><p className="font-semibold">{course.rating?.toFixed(1) ?? "—"}</p></div>
        </CardContent></Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Modules</CardTitle>
          <Button size="sm" className="gap-2" onClick={() => setModuleDialogOpen(true)}>
            <Plus className="h-4 w-4" /> Add module
          </Button>
        </CardHeader>
        <CardContent>
          {modulesFetch.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
            </div>
          ) : modulesFetch.error ? (
            <ErrorState message={modulesFetch.error} onRetry={modulesFetch.refetch} />
          ) : !modulesFetch.data || modulesFetch.data.length === 0 ? (
            <EmptyState
              icon={Layers}
              title="No modules yet"
              description="Break this course into modules and lessons."
              actionLabel="Add first module"
              onAction={() => setModuleDialogOpen(true)}
            />
          ) : (
            <div className="divide-y">
              {modulesFetch.data.map((m, idx) => (
                <div key={m.id} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <span className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10 text-sm font-semibold text-primary">
                      {idx + 1}
                    </span>
                    <p className="font-medium">{m.title}</p>
                  </div>
                  <p className="text-sm text-muted-foreground">{m.lessonCount} lessons</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <CreateModuleDialog open={moduleDialogOpen} onOpenChange={setModuleDialogOpen} onCreate={handleCreateModule} />
    </div>
  );
}
