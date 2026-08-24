"use client";

import { useState } from "react";
import { ShieldAlert, Search, X } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ReportPagination } from "@/components/reports/report-pagination";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { auditLogsApi } from "@/lib/api/administration";
import { Role } from "@/lib/constants";
import { formatDate, formatTime } from "@/lib/utils";
import { AuditLogEntry } from "@/types";

const PAGE_SIZE = 20;

export default function AuditLogsPage() {
  const currentUser = useAuthStore((s) => s.user);

  const [page, setPage] = useState(0);
  const [action, setAction] = useState("");
  const [entityType, setEntityType] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [appliedFilters, setAppliedFilters] = useState({ action: "", entityType: "", from: "", to: "" });

  const { data, isLoading, error, refetch } = useFetch(
    () =>
      auditLogsApi.search({
        page,
        size: PAGE_SIZE,
        action: appliedFilters.action || undefined,
        entityType: appliedFilters.entityType || undefined,
        from: appliedFilters.from ? new Date(appliedFilters.from).toISOString() : undefined,
        to: appliedFilters.to ? new Date(appliedFilters.to).toISOString() : undefined,
      }),
    [page, appliedFilters]
  );

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can view the audit log history." />;
  }

  function applyFilters() {
    setPage(0);
    setAppliedFilters({ action, entityType, from, to });
  }

  function clearFilters() {
    setAction("");
    setEntityType("");
    setFrom("");
    setTo("");
    setPage(0);
    setAppliedFilters({ action: "", entityType: "", from: "", to: "" });
  }

  const hasActiveFilters = Object.values(appliedFilters).some(Boolean);

  const columns: Column<AuditLogEntry>[] = [
    { key: "createdAt", header: "When", render: (r) => `${formatDate(r.createdAt)} ${formatTime(r.createdAt)}` },
    { key: "actor", header: "Actor", render: (r) => (
      <div>
        <p className="font-medium">{r.actorName}</p>
        {r.actorEmail && <p className="text-xs text-muted-foreground">{r.actorEmail}</p>}
      </div>
    ) },
    { key: "action", header: "Action", render: (r) => <span className="font-mono text-xs">{r.action}</span> },
    { key: "entity", header: "Entity", render: (r) => (r.entityType ? `${r.entityType}${r.entityId ? ` · ${r.entityId.slice(0, 8)}…` : ""}` : "—") },
    { key: "ipAddress", header: "IP Address", render: (r) => r.ipAddress ?? "—" },
  ];

  return (
    <div>
      <PageHeader
        title="Audit Log History"
        description="Searchable record of every important user and administrative action, with timestamps."
      />

      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <div className="space-y-1.5">
              <Label htmlFor="action">Action contains</Label>
              <Input id="action" placeholder="e.g. DELETE_COURSE" value={action} onChange={(e) => setAction(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="entityType">Entity type</Label>
              <Input id="entityType" placeholder="e.g. Course" value={entityType} onChange={(e) => setEntityType(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="from">From</Label>
              <Input id="from" type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="to">To</Label>
              <Input id="to" type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} />
            </div>
            <div className="flex items-end gap-2">
              <Button onClick={applyFilters} className="gap-2">
                <Search className="h-4 w-4" /> Search
              </Button>
              {hasActiveFilters && (
                <Button variant="outline" onClick={clearFilters} className="gap-2">
                  <X className="h-4 w-4" /> Clear
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {error && <ErrorState message={error} onRetry={refetch} />}

      {!error && (
        <>
          <DataTable
            columns={columns}
            data={data?.content ?? []}
            isLoading={isLoading}
            rowKey={(r) => r.id}
            emptyTitle="No audit log entries found"
            emptyDescription="Try widening your filters, or check back after more admin actions occur."
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
