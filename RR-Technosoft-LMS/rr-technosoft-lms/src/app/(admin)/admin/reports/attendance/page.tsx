"use client";

import { useState } from "react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ReportsSubNav } from "@/components/reports/reports-sub-nav";
import { ReportPagination } from "@/components/reports/report-pagination";
import { ExportButtons } from "@/components/reports/export-buttons";
import { CourseFilterSelect } from "@/components/reports/course-filter-select";
import { useFetch } from "@/hooks/use-fetch";
import { reportsApi } from "@/lib/api/reports";
import { AttendanceReportRow } from "@/types";

const PAGE_SIZE = 20;

export default function AttendanceReportsPage() {
  const [courseId, setCourseId] = useState<string | undefined>(undefined);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);

  const filters = { courseId, from: from || undefined, to: to || undefined, page, size: PAGE_SIZE };

  const { data, isLoading, error, refetch } = useFetch(
    () => reportsApi.attendance(filters),
    [courseId, from, to, page]
  );

  const columns: Column<AttendanceReportRow>[] = [
    { key: "course", header: "Course", render: (r) => <span className="font-medium">{r.courseTitle}</span> },
    { key: "sessions", header: "Sessions Held", render: (r) => r.sessionsHeld },
    { key: "present", header: "Present", render: (r) => r.presentCount },
    { key: "absent", header: "Absent", render: (r) => r.absentCount },
    { key: "late", header: "Late", render: (r) => r.lateCount },
    { key: "excused", header: "Excused", render: (r) => r.excusedCount },
    { key: "pct", header: "Attendance %", render: (r) => `${r.attendancePercentage.toFixed(1)}%` },
  ];

  return (
    <div>
      <PageHeader
        title="Attendance Reports"
        description="Per-course attendance rollup over the selected date range."
        actions={
          <ExportButtons
            onExportExcel={() => reportsApi.exportAttendanceExcel({ courseId, from: from || undefined, to: to || undefined })}
            onExportPdf={() => reportsApi.exportAttendancePdf({ courseId, from: from || undefined, to: to || undefined })}
          />
        }
      />
      <ReportsSubNav />

      <Card className="mb-4">
        <CardContent className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-3">
          <div className="space-y-1.5">
            <Label>Course</Label>
            <CourseFilterSelect
              value={courseId}
              onChange={(v) => {
                setCourseId(v);
                setPage(0);
              }}
            />
          </div>
          <div className="space-y-1.5">
            <Label>From</Label>
            <Input
              type="date"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <div className="space-y-1.5">
            <Label>To</Label>
            <Input
              type="date"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </CardContent>
      </Card>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <>
          <DataTable
            columns={columns}
            data={data?.content ?? []}
            isLoading={isLoading}
            rowKey={(r) => r.courseId}
            emptyTitle="No attendance records match these filters"
          />
          {data && (
            <ReportPagination
              page={data.number}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onPageChange={setPage}
            />
          )}
        </>
      )}
    </div>
  );
}
