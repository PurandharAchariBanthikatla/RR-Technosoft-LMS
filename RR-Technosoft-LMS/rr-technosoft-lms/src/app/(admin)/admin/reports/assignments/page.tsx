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
import { AssignmentReportRow } from "@/types";
import { formatDate } from "@/lib/utils";

const PAGE_SIZE = 20;

/** type="date" input gives "yyyy-MM-dd" — widen to a full-day ISO OffsetDateTime range for the API. */
function toRangeStart(date: string) {
  return date ? `${date}T00:00:00Z` : undefined;
}
function toRangeEnd(date: string) {
  return date ? `${date}T23:59:59Z` : undefined;
}

export default function AssignmentReportsPage() {
  const [courseId, setCourseId] = useState<string | undefined>(undefined);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);

  const filters = { courseId, from: toRangeStart(from), to: toRangeEnd(to), page, size: PAGE_SIZE };

  const { data, isLoading, error, refetch } = useFetch(
    () => reportsApi.assignments(filters),
    [courseId, from, to, page]
  );

  const columns: Column<AssignmentReportRow>[] = [
    {
      key: "assignment",
      header: "Assignment",
      render: (r) => (
        <div>
          <p className="font-medium">{r.assignmentTitle}</p>
          <p className="text-xs text-muted-foreground">{r.courseTitle ?? "—"}</p>
        </div>
      ),
    },
    { key: "due", header: "Due", render: (r) => (r.dueAt ? formatDate(r.dueAt) : "—") },
    { key: "total", header: "Total Students", render: (r) => r.totalStudents },
    { key: "submitted", header: "Submitted", render: (r) => r.submittedCount },
    { key: "graded", header: "Graded", render: (r) => r.gradedCount },
    { key: "late", header: "Late", render: (r) => r.lateCount },
    { key: "pending", header: "Pending", render: (r) => r.pendingCount },
    { key: "avgScore", header: "Avg Score", render: (r) => r.avgScore.toFixed(1) },
    { key: "rate", header: "Submission Rate", render: (r) => `${r.submissionRatePercentage.toFixed(1)}%` },
  ];

  return (
    <div>
      <PageHeader
        title="Assignment Reports"
        description="Submission and grading progress per assignment."
        actions={
          <ExportButtons
            onExportExcel={() => reportsApi.exportAssignmentsExcel({ courseId, from: filters.from, to: filters.to })}
            onExportPdf={() => reportsApi.exportAssignmentsPdf({ courseId, from: filters.from, to: filters.to })}
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
            <Label>Due from</Label>
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
            <Label>Due to</Label>
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
            rowKey={(r) => r.assignmentId}
            emptyTitle="No assignments match these filters"
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
