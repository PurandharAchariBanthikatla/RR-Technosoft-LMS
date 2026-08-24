"use client";

import Link from "next/link";
import { FileCheck2 } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { formatDate } from "@/lib/utils";
import { PlacementApplication } from "@/types";

export default function MyApplicationsPage() {
  const { data, isLoading, error, refetch } = useFetch(() => placementsApi.myApplications({ page: 0, size: 50 }), []);

  const columns: Column<PlacementApplication>[] = [
    {
      key: "drive",
      header: "Company / Role",
      render: (a) => (
        <Link href={`/student/placements/${a.placementId}`} className="hover:underline">
          <p className="font-medium">{a.companyName}</p>
          <p className="text-xs text-muted-foreground">{a.role}</p>
        </Link>
      ),
    },
    { key: "applied", header: "Applied on", render: (a) => formatDate(a.appliedAt) },
    { key: "status", header: "Status", render: (a) => <StatusBadge status={a.status} /> },
  ];

  return (
    <div>
      <PageHeader title="My applications" description="Track the status of every drive you've applied to." />
      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.content ?? []}
          isLoading={isLoading}
          rowKey={(a) => a.id}
          emptyTitle="No applications yet"
          emptyDescription="Browse open drives and apply to see them here."
        />
      )}
    </div>
  );
}
