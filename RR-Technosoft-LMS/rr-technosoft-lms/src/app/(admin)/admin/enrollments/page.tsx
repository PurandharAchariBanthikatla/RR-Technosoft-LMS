"use client";

import { useState } from "react";
import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Progress } from "@/components/ui/progress";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { enrollmentsApi } from "@/lib/api/enrollments";
import { formatDate } from "@/lib/utils";
import { Enrollment } from "@/types";

export default function AdminEnrollmentsPage() {
  const [status, setStatus] = useState("all");
  const { data, isLoading, error, refetch } = useFetch(
    () => enrollmentsApi.list({ status: status === "all" ? undefined : status, page: 0, size: 20 }),
    [status]
  );

  const columns: Column<Enrollment>[] = [
    { key: "student", header: "Student", render: (e) => e.studentName },
    { key: "course", header: "Course", render: (e) => e.courseTitle },
    {
      key: "progress",
      header: "Progress",
      render: (e) => (
        <div className="flex w-40 items-center gap-2">
          <Progress value={e.progress} className="h-1.5" />
          <span className="w-9 text-xs text-muted-foreground">{e.progress}%</span>
        </div>
      ),
    },
    { key: "status", header: "Status", render: (e) => <StatusBadge status={e.status} /> },
    { key: "enrolledAt", header: "Enrolled on", render: (e) => formatDate(e.enrolledAt) },
  ];

  return (
    <div>
      <PageHeader title="Enrollments" description="Track every student's enrollment status and course progress." />

      <div className="mb-4">
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-full sm:w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            <SelectItem value="ACTIVE">Active</SelectItem>
            <SelectItem value="COMPLETED">Completed</SelectItem>
            <SelectItem value="PENDING">Pending</SelectItem>
            <SelectItem value="DROPPED">Dropped</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(e) => e.id} emptyTitle="No enrollments found" />
      )}
    </div>
  );
}
