"use client";

import Link from "next/link";
import { Briefcase, Plus, Building2, Users, TrendingUp } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { StatCard } from "@/components/shared/stat-card";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { formatDate } from "@/lib/utils";
import { PlacementDrive } from "@/types";

export default function AdminPlacementsPage() {
  const { data, isLoading, error, refetch } = useFetch(() => placementsApi.list({ page: 0, size: 20 }), []);
  const { data: dashboard } = useFetch(() => placementsApi.dashboard(), []);

  const columns: Column<PlacementDrive>[] = [
    {
      key: "company",
      header: "Company / Role",
      render: (p) => (
        <Link href={`/admin/placements/${p.id}`} className="flex items-center gap-2 hover:underline">
          <Briefcase className="h-4 w-4 text-primary" />
          <div>
            <p className="font-medium">{p.companyName}</p>
            <p className="text-xs text-muted-foreground">{p.role}</p>
          </div>
        </Link>
      ),
    },
    { key: "package", header: "Package", render: (p) => p.packageLpa ?? "—" },
    { key: "location", header: "Location", render: (p) => p.location ?? "—" },
    { key: "date", header: "Drive date", render: (p) => (p.driveDate ? formatDate(p.driveDate) : "—") },
    { key: "deadline", header: "Apply by", render: (p) => formatDate(p.lastDateToApply) },
    { key: "applicants", header: "Applicants", render: (p) => p.applicantsCount ?? 0 },
    { key: "status", header: "Status", render: (p) => <StatusBadge status={p.status} /> },
  ];

  return (
    <div>
      <PageHeader
        title="Placements"
        description="Coordinate placement drives and track applicants."
        actions={
          <Button asChild className="gap-2">
            <Link href="/admin/placements/new"><Plus className="h-4 w-4" /> New drive</Link>
          </Button>
        }
      />

      {dashboard && (
        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label="Open drives" value={dashboard.openDrives} icon={Briefcase} />
          <StatCard label="Active companies" value={dashboard.activeCompanies} icon={Building2} />
          <StatCard label="Total applications" value={dashboard.totalApplications} icon={Users} />
          <StatCard label="Placement rate" value={`${dashboard.placementRate.toFixed(1)}%`} icon={TrendingUp} accent="success" />
        </div>
      )}

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(p) => p.id} emptyTitle="No placement drives yet" />
      )}
    </div>
  );
}
