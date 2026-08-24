"use client";

import { useState } from "react";
import { Wallet, Receipt, TrendingUp } from "lucide-react";
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from "recharts";

import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ReportsSubNav } from "@/components/reports/reports-sub-nav";
import { ReportPagination } from "@/components/reports/report-pagination";
import { ExportButtons } from "@/components/reports/export-buttons";
import { CourseFilterSelect } from "@/components/reports/course-filter-select";
import { useFetch } from "@/hooks/use-fetch";
import { reportsApi } from "@/lib/api/reports";
import { RevenueReportRow } from "@/types";

const PAGE_SIZE = 20;

function toRangeStart(date: string) {
  return date ? `${date}T00:00:00Z` : undefined;
}
function toRangeEnd(date: string) {
  return date ? `${date}T23:59:59Z` : undefined;
}

export default function RevenueReportsPage() {
  const [courseId, setCourseId] = useState<string | undefined>(undefined);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);

  const filters = { courseId, from: toRangeStart(from), to: toRangeEnd(to), page, size: PAGE_SIZE };

  const { data, isLoading, error, refetch } = useFetch(
    () => reportsApi.revenue(filters),
    [courseId, from, to, page]
  );
  const { data: summary, isLoading: summaryLoading } = useFetch(
    () => reportsApi.revenueSummary({ from: filters.from, to: filters.to }),
    [filters.from, filters.to]
  );

  const columns: Column<RevenueReportRow>[] = [
    {
      key: "course",
      header: "Course",
      render: (r) => (
        <div>
          <p className="font-medium">{r.courseTitle}</p>
          <p className="text-xs text-muted-foreground">{r.category ?? "—"}</p>
        </div>
      ),
    },
    { key: "price", header: "Unit Price", render: (r) => `₹${r.unitPrice.toLocaleString("en-IN")}` },
    { key: "paid", header: "Paid Enrollments", render: (r) => r.paidEnrollments },
    { key: "other", header: "Dropped/Pending", render: (r) => r.droppedOrPendingEnrollments },
    { key: "revenue", header: "Total Revenue", render: (r) => `₹${r.totalRevenue.toLocaleString("en-IN")}` },
  ];

  return (
    <div>
      <PageHeader
        title="Revenue Reports"
        description="Revenue per course, derived from paid (active/completed) enrollments × course price."
        actions={
          <ExportButtons
            onExportExcel={() => reportsApi.exportRevenueExcel({ courseId, from: filters.from, to: filters.to })}
            onExportPdf={() => reportsApi.exportRevenuePdf({ courseId, from: filters.from, to: filters.to })}
          />
        }
      />
      <ReportsSubNav />

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        {summaryLoading ? (
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-lg" />)
        ) : (
          <>
            <StatCard label="Total Revenue" value={`₹${(summary?.totalRevenue ?? 0).toLocaleString("en-IN")}`} icon={Wallet} />
            <StatCard label="Paid Enrollments" value={summary?.totalPaidEnrollments ?? 0} icon={Receipt} />
            <StatCard
              label="Avg Order Value"
              value={`₹${(summary?.averageOrderValue ?? 0).toLocaleString("en-IN")}`}
              icon={TrendingUp}
            />
          </>
        )}
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Monthly revenue trend</CardTitle>
          <CardDescription>Revenue from paid enrollments over the selected window</CardDescription>
        </CardHeader>
        <CardContent>
          {summaryLoading ? (
            <Skeleton className="h-64 w-full" />
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <AreaChart data={summary?.monthlyTrend ?? []}>
                <defs>
                  <linearGradient id="colorRevenueTrend" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#E31E24" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#E31E24" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                <YAxis tickLine={false} axisLine={false} fontSize={12} />
                <Tooltip
                  contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }}
                  formatter={(v: number) => [`₹${v.toLocaleString("en-IN")}`, "Revenue"]}
                />
                <Area type="monotone" dataKey="value" stroke="#E31E24" strokeWidth={2} fill="url(#colorRevenueTrend)" />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

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
            <Label>Enrolled from</Label>
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
            <Label>Enrolled to</Label>
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
            emptyTitle="No revenue records match these filters"
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
