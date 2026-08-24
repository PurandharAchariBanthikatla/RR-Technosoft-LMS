"use client";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { attendanceApi } from "@/lib/api/attendance";
import { formatDate } from "@/lib/utils";
import { AttendanceRecord } from "@/types";
import { CalendarCheck } from "lucide-react";

export default function AdminAttendancePage() {
  const { data, isLoading, error, refetch } = useFetch(() => attendanceApi.list({ page: 0, size: 20 }), []);

  const columns: Column<AttendanceRecord>[] = [
    { key: "student", header: "Student", render: (a) => a.studentName ?? "—" },
    { key: "course", header: "Course", render: (a) => a.courseTitle },
    { key: "date", header: "Date", render: (a) => formatDate(a.date) },
    { key: "status", header: "Status", render: (a) => <StatusBadge status={a.status} /> },
  ];

  return (
    <div>
      <PageHeader
        title="Attendance"
        description="Review and mark attendance for every batch and session."
        actions={<Button className="gap-2"><CalendarCheck className="h-4 w-4" /> Mark attendance</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(a) => a.id} emptyTitle="No attendance records found" />
      )}
    </div>
  );
}
