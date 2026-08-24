"use client";

import Link from "next/link";
import { HelpCircle } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { quizzesApi } from "@/lib/api/quizzes";
import { formatDate } from "@/lib/utils";

export default function StudentQuizzesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => quizzesApi.list({ page: 0, size: 20 }), []);

  return (
    <div>
      <PageHeader title="Quizzes" description="Test your knowledge and track your scores." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-32 rounded-lg" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={HelpCircle} title="No quizzes available" description="Quizzes for your courses will appear here." />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {data.content.map((q) => (
            <Card key={q.id}>
              <CardContent className="p-5">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-display font-semibold">{q.title}</p>
                    <p className="text-sm text-muted-foreground">{q.courseTitle}</p>
                  </div>
                  {q.attempted && q.score !== undefined && (
                    <span className="rounded-full bg-success/10 px-2.5 py-1 text-xs font-medium text-success">
                      {q.score}/{q.totalMarks}
                    </span>
                  )}
                </div>
                <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                  <span>{q.totalQuestions} questions</span>
                  <span>{q.durationMinutes} min</span>
                  <span>Till {formatDate(q.availableTo)}</span>
                </div>
                <Button asChild size="sm" className="mt-4 w-full" variant={q.attempted ? "outline" : "default"}>
                  <Link href={`/student/quizzes/${q.id}`}>{q.attempted ? "View result" : "Start quiz"}</Link>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
