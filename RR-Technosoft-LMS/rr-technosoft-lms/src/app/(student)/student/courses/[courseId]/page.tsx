"use client";

import { useParams } from "next/navigation";
import { PlayCircle, FileText, Paperclip, CheckCircle2 } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { Progress } from "@/components/ui/progress";
import { useFetch } from "@/hooks/use-fetch";
import { coursesApi } from "@/lib/api/courses";
import { cn } from "@/lib/utils";
import { LessonType } from "@/types";

const LESSON_ICON: Record<LessonType, typeof PlayCircle> = {
  VIDEO: PlayCircle,
  ARTICLE: FileText,
  RESOURCE: Paperclip,
};

export default function StudentCourseDetailPage() {
  const params = useParams<{ courseId: string }>();
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

  return (
    <div>
      <PageHeader title={course.title} description={`Taught by ${course.instructorName}`} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader><CardTitle>Course content</CardTitle></CardHeader>
            <CardContent>
              {modulesFetch.isLoading ? (
                <div className="space-y-2">
                  {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
                </div>
              ) : modulesFetch.error ? (
                <ErrorState message={modulesFetch.error} onRetry={modulesFetch.refetch} />
              ) : (
                <ModuleAccordionList courseId={course.id} moduleList={modulesFetch.data ?? []} />
              )}
            </CardContent>
          </Card>
        </div>

        <Card className="h-fit">
          <CardHeader><CardTitle>Your progress</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <Progress value={42} className="h-2" />
            <p className="text-sm text-muted-foreground">42% complete · keep going!</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ModuleAccordionList({ moduleList }: { courseId: string; moduleList: { id: string; title: string; lessonCount: number }[] }) {
  if (moduleList.length === 0) {
    return <p className="py-8 text-center text-sm text-muted-foreground">Course content will appear here once modules are published.</p>;
  }

  return (
    <div className="divide-y">
      {moduleList.map((m, idx) => (
        <div key={m.id} className="py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10 text-sm font-semibold text-primary">
                {idx + 1}
              </span>
              <p className="font-medium">{m.title}</p>
            </div>
            <p className="text-sm text-muted-foreground">{m.lessonCount} lessons</p>
          </div>
        </div>
      ))}
    </div>
  );
}

// Re-export for potential direct lesson-row usage elsewhere
function LessonRow({ title, type, durationMinutes, completed }: { title: string; type: LessonType; durationMinutes: number; completed?: boolean }) {
  const Icon = LESSON_ICON[type];
  return (
    <div className="flex items-center justify-between rounded-md px-3 py-2 hover:bg-muted/50">
      <div className="flex items-center gap-3">
        <Icon className="h-4 w-4 text-muted-foreground" />
        <span className={cn("text-sm", completed && "text-muted-foreground line-through")}>{title}</span>
      </div>
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        {durationMinutes} min
        {completed && <CheckCircle2 className="h-4 w-4 text-success" />}
      </div>
    </div>
  );
}
