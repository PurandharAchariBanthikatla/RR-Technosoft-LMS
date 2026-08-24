"use client";

import { useState } from "react";
import { Search } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ReportsSubNav } from "@/components/reports/reports-sub-nav";
import { ReportPagination } from "@/components/reports/report-pagination";
import { ExportButtons } from "@/components/reports/export-buttons";
import { useFetch } from "@/hooks/use-fetch";
import { useDebounce } from "@/hooks/use-debounce";
import { reportsApi } from "@/lib/api/reports";
import { StudentReportRow } from "@/types";

const PAGE_SIZE = 20;

export default function StudentReportsPage() {
  const [search, setSearch] = useState("");
  const [batch, setBatch] = useState("");
  const [branch, setBranch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search);
  const debouncedBatch = useDebounce(batch);
  const debouncedBranch = useDebounce(branch);

  const filters = {
    search: debouncedSearch || undefined,
    batch: debouncedBatch || undefined,
    branch: debouncedBranch || undefined,
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading, error, refetch } = useFetch(
    () => reportsApi.students(filters),
    [debouncedSearch, debouncedBatch, debouncedBranch, page]
  );

  function resetToFirstPage(setter: (v: string) => void) {
    return (value: string) => {
      setter(value);
      setPage(0);
    };
  }

  const columns: Column<StudentReportRow>[] = [
    {
      key: "student",
      header: "Student",
      render: (r) => (
        <div>
          <p className="font-medium">{r.fullName}</p>
          <p className="text-xs text-muted-foreground font-mono">{r.studentCode ?? "—"}</p>
        </div>
      ),
    },
    { key: "batch", header: "Batch", render: (r) => r.batch ?? "—" },
    { key: "branch", header: "Branch", render: (r) => r.branch ?? "—" },
    { key: "courses", header: "Courses", render: (r) => r.coursesEnrolled },
    { key: "progress", header: "Avg Progress", render: (r) => `${r.avgProgressPercentage.toFixed(1)}%` },
    { key: "attendance", header: "Attendance", render: (r) => `${r.attendancePercentage.toFixed(1)}%` },
    { key: "score", header: "Avg Score", render: (r) => r.avgAssignmentScore.toFixed(1) },
    {
      key: "assignments",
      header: "Assignments",
      render: (r) => `${r.assignmentsSubmitted} submitted · ${r.assignmentsPending} pending`,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Student Reports"
        description="Per-student rollup of enrollment, attendance and assignment performance."
        actions={
          <ExportButtons
            onExportExcel={() => reportsApi.exportStudentsExcel({ search: filters.search, batch: filters.batch, branch: filters.branch })}
            onExportPdf={() => reportsApi.exportStudentsPdf({ search: filters.search, batch: filters.batch, branch: filters.branch })}
          />
        }
      />
      <ReportsSubNav />

      <Card className="mb-4">
        <CardContent className="flex flex-col gap-3 p-4 sm:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name or Student ID..."
              className="pl-8"
              value={search}
              onChange={(e) => resetToFirstPage(setSearch)(e.target.value)}
            />
          </div>
          <Input
            placeholder="Batch (e.g. 2026-A)"
            className="sm:w-48"
            value={batch}
            onChange={(e) => resetToFirstPage(setBatch)(e.target.value)}
          />
          <Input
            placeholder="Branch (e.g. CSE)"
            className="sm:w-48"
            value={branch}
            onChange={(e) => resetToFirstPage(setBranch)(e.target.value)}
          />
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
            rowKey={(r) => r.studentId}
            emptyTitle="No students match these filters"
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
