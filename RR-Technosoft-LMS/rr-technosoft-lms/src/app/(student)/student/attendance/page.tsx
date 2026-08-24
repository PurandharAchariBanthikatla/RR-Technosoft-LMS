"use client";

import { CalendarCheck } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { DataTable, type Column } from "@/components/shared/data-table";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { ProgressRing } from "@/components/dashboard/progress-ring";
import { useFetch } from "@/hooks/use-fetch";
import { attendanceApi } from "@/lib/api/attendance";
import { formatDate } from "@/lib/utils";
import { AttendanceRecord } from "@/types";
import { StatusBadge } from "@/components/shared/status-badge";

export default function StudentAttendancePage() {
  const summaryFetch = useFetch(() => attendanceApi.summary(), []);
  const recordsFetch = useFetch(() => attendanceApi.mine(), []);

  const columns: Column<AttendanceRecord>[] = [
    { key: "date", header: "Date", render: (a) => formatDate(a.date) },
    { key: "course", header: "Course", render: (a) => a.courseTitle },
    { key: "status", header: "Status", render: (a) => <StatusBadge status={a.status} /> },
  ];

  return (
    <div>
      <PageHeader title="Attendance" description="Your session attendance across all enrolled courses." />

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-4">
        <Card className="sm:col-span-1 flex flex-col items-center justify-center gap-2 p-6">
          {summaryFetch.isLoading ? (
            <Skeleton className="h-24 w-24 rounded-full" />
          ) : (
            <ProgressRing value={summaryFetch.data?.percentage ?? 0} size={100} label="attendance" />
          )}
        </Card>
        <Card className="sm:col-span-3">
          <CardContent className="grid grid-cols-3 gap-4 p-6 text-center">
            <div>
              <p className="font-display text-2xl font-bold">{summaryFetch.data?.totalClasses ?? "—"}</p>
              <p className="text-xs text-muted-foreground">Total classes</p>
            </div>
            <div>
              <p className="font-display text-2xl font-bold text-success">{summaryFetch.data?.present ?? "—"}</p>
              <p className="text-xs text-muted-foreground">Present</p>
            </div>
            <div>
              <p className="font-display text-2xl font-bold text-destructive">{summaryFetch.data?.absent ?? "—"}</p>
              <p className="text-xs text-muted-foreground">Absent</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {recordsFetch.error ? (
        <ErrorState message={recordsFetch.error} onRetry={recordsFetch.refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={recordsFetch.data ?? []}
          isLoading={recordsFetch.isLoading}
          rowKey={(a) => a.id}
          emptyTitle="No attendance records yet"
        />
      )}
    </div>
  );
}
