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
import { FacultyReportRow } from "@/types";

const PAGE_SIZE = 20;

export default function FacultyReportsPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search);

  const filters = { search: debouncedSearch || undefined, page, size: PAGE_SIZE };

  const { data, isLoading, error, refetch } = useFetch(() => reportsApi.faculty(filters), [debouncedSearch, page]);

  const columns: Column<FacultyReportRow>[] = [
    { key: "instructor", header: "Instructor", render: (r) => <span className="font-medium">{r.instructorName}</span> },
    { key: "courses", header: "Courses Handled", render: (r) => r.coursesHandled },
    { key: "students", header: "Total Students", render: (r) => r.totalStudents },
    { key: "rating", header: "Avg Rating", render: (r) => (r.avgCourseRating ? r.avgCourseRating.toFixed(1) : "—") },
    { key: "completion", header: "Avg Completion", render: (r) => `${r.avgCompletionPercentage.toFixed(1)}%` },
    { key: "revenue", header: "Revenue Generated", render: (r) => `₹${r.revenueGenerated.toLocaleString("en-IN")}` },
  ];

  return (
    <div>
      <PageHeader
        title="Faculty Reports"
        description="Course load, student reach and outcomes per instructor."
        actions={
          <ExportButtons
            onExportExcel={() => reportsApi.exportFacultyExcel({ search: filters.search })}
            onExportPdf={() => reportsApi.exportFacultyPdf({ search: filters.search })}
          />
        }
      />
      <ReportsSubNav />

      <Card className="mb-4">
        <CardContent className="p-4">
          <div className="relative max-w-sm">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by instructor name..."
              className="pl-8"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
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
            rowKey={(r) => r.instructorName}
            emptyTitle="No instructors found"
            emptyDescription="Courses need an instructor name set before they show up here."
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
