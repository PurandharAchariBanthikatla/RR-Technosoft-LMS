"use client";

import { Plus } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { quizzesApi } from "@/lib/api/quizzes";
import { formatDate } from "@/lib/utils";
import { Quiz } from "@/types";

export default function AdminQuizzesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => quizzesApi.list({ page: 0, size: 20 }), []);

  const columns: Column<Quiz>[] = [
    {
      key: "title",
      header: "Quiz",
      render: (q) => (
        <div>
          <p className="font-medium">{q.title}</p>
          <p className="text-xs text-muted-foreground">{q.courseTitle}</p>
        </div>
      ),
    },
    { key: "questions", header: "Questions", render: (q) => q.totalQuestions },
    { key: "duration", header: "Duration", render: (q) => `${q.durationMinutes} min` },
    { key: "marks", header: "Total marks", render: (q) => q.totalMarks },
    { key: "window", header: "Available", render: (q) => `${formatDate(q.availableFrom)} – ${formatDate(q.availableTo)}` },
  ];

  return (
    <div>
      <PageHeader
        title="Quizzes"
        description="Manage quizzes and assessment windows for each course."
        actions={<Button className="gap-2"><Plus className="h-4 w-4" /> New quiz</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(q) => q.id} emptyTitle="No quizzes yet" />
      )}
    </div>
  );
}
