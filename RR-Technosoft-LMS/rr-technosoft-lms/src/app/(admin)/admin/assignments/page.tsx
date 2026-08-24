"use client";

import { Plus } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { assignmentsApi } from "@/lib/api/assignments";
import { formatDate } from "@/lib/utils";
import { Assignment } from "@/types";

export default function AdminAssignmentsPage() {
  const { data, isLoading, error, refetch } = useFetch(() => assignmentsApi.list({ page: 0, size: 20 }), []);

  const columns: Column<Assignment>[] = [
    {
      key: "title",
      header: "Assignment",
      render: (a) => (
        <div>
          <p className="font-medium">{a.title}</p>
          <p className="text-xs text-muted-foreground">{a.courseTitle}</p>
        </div>
      ),
    },
    { key: "due", header: "Due date", render: (a) => formatDate(a.dueDate) },
    { key: "maxScore", header: "Max score", render: (a) => a.maxScore },
    {
      key: "submissions",
      header: "Submissions",
      render: (a) => `${a.submittedCount ?? 0}/${a.totalStudents ?? 0}`,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Assignments"
        description="Create and track assignments across all courses."
        actions={<Button className="gap-2"><Plus className="h-4 w-4" /> New assignment</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(a) => a.id} emptyTitle="No assignments yet" />
      )}
    </div>
  );
}
