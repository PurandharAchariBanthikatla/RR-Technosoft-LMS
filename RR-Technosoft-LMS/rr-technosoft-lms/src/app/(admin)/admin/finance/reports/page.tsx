"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Wallet, TrendingUp, AlertTriangle, Layers } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { financeReportsApi } from "@/lib/api/finance";
import { coursesApi } from "@/lib/api/courses";
import { formatCurrency, formatDate } from "@/lib/utils";
import { StudentFee, FeeStatus } from "@/types";

const STATUS_OPTIONS: FeeStatus[] = ["PENDING", "PARTIAL", "PAID", "OVERDUE", "WAIVED", "CANCELLED"];

export default function AdminFinanceReportsPage() {
  const router = useRouter();
  const [courseId, setCourseId] = useState<string>("all");
  const [status, setStatus] = useState<string>("all");

  const { data: summary, isLoading: summaryLoading, error: summaryError, refetch: refetchSummary } = useFetch(
    () => financeReportsApi.summary(courseId === "all" ? undefined : courseId),
    [courseId]
  );
  const { data: rows, isLoading: rowsLoading, error: rowsError, refetch: refetchRows } = useFetch(
    () =>
      financeReportsApi.studentFees({
        courseId: courseId === "all" ? undefined : courseId,
        status: status === "all" ? undefined : (status as FeeStatus),
        page: 0,
        size: 50,
      }),
    [courseId, status]
  );
  const { data: courses } = useFetch(() => coursesApi.list({ page: 0, size: 200 }), []);

  const columns: Column<StudentFee>[] = [
    { key: "student", header: "Student", render: (f) => f.studentName },
    { key: "course", header: "Course", render: (f) => f.courseTitle ?? "—" },
    { key: "net", header: "Net payable", render: (f) => formatCurrency(f.netPayable, f.currency) },
    { key: "paid", header: "Paid", render: (f) => formatCurrency(f.amountPaid, f.currency) },
    { key: "balance", header: "Balance due", render: (f) => formatCurrency(f.balanceDue, f.currency) },
    { key: "status", header: "Status", render: (f) => <StatusBadge status={f.status} /> },
    { key: "created", header: "Assigned", render: (f) => formatDate(f.createdAt) },
  ];

  return (
    <div>
      <PageHeader title="Finance Reports" description="Collection summary and a filterable ledger of every student fee record." />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row">
        <Select value={courseId} onValueChange={setCourseId}>
          <SelectTrigger className="w-full sm:w-56"><SelectValue placeholder="Course" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All courses</SelectItem>
            {courses?.content.map((c) => (
              <SelectItem key={c.id} value={c.id}>{c.title}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-full sm:w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUS_OPTIONS.map((s) => (
              <SelectItem key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {summaryError ? (
        <ErrorState message={summaryError} onRetry={refetchSummary} />
      ) : summaryLoading || !summary ? (
        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 w-full" />)}
        </div>
      ) : (
        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard icon={Wallet} label="Total billed" value={formatCurrency(summary.totalBilled)} />
          <StatCard icon={TrendingUp} label="Total collected" value={formatCurrency(summary.totalCollected)} accent="text-emerald-600" />
          <StatCard icon={AlertTriangle} label="Outstanding" value={formatCurrency(summary.totalOutstanding)} accent="text-amber-600" />
          <StatCard icon={Layers} label="Overdue records" value={String(summary.overdueCount)} accent="text-destructive" />
        </div>
      )}

      <h2 className="mb-2 font-display text-lg font-semibold">Student fee ledger</h2>
      {rowsError ? (
        <ErrorState message={rowsError} onRetry={refetchRows} />
      ) : (
        <DataTable
          columns={columns}
          data={rows?.content ?? []}
          isLoading={rowsLoading}
          rowKey={(f) => f.id}
          emptyTitle="No records match these filters"
          onRowClick={(f) => router.push(`/admin/finance/student-fees/${f.id}`)}
        />
      )}
    </div>
  );
}

function StatCard({
  icon: Icon, label, value, accent,
}: { icon: typeof Wallet; label: string; value: string; accent?: string }) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-xs font-medium text-muted-foreground">{label}</CardTitle>
        <Icon className={`h-4 w-4 text-muted-foreground ${accent ?? ""}`} />
      </CardHeader>
      <CardContent>
        <p className={`text-2xl font-bold ${accent ?? ""}`}>{value}</p>
      </CardContent>
    </Card>
  );
}
