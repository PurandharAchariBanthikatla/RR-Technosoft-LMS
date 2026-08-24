"use client";

import Link from "next/link";
import { Briefcase, MapPin } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { formatDate } from "@/lib/utils";

export default function StudentPlacementsPage() {
  const { data, isLoading, error, refetch } = useFetch(() => placementsApi.list({ page: 0, size: 20, status: "OPEN" }), []);

  return (
    <div>
      <PageHeader
        title="Placements"
        description="Explore open placement drives and track your applications."
        actions={
          <Button asChild variant="outline">
            <Link href="/student/placements/applications">My applications</Link>
          </Button>
        }
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-24 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={Briefcase} title="No drives available" description="New placement drives will appear here." />
      ) : (
        <div className="space-y-3">
          {data.content.map((p) => (
            <Link key={p.id} href={`/student/placements/${p.id}`}>
              <Card className="transition-colors hover:border-primary">
                <CardContent className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-display font-semibold">{p.companyName}</p>
                      <StatusBadge status={p.status} />
                    </div>
                    <p className="text-sm text-muted-foreground">{p.role} · {p.packageLpa ?? "Not disclosed"}</p>
                    <div className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                      <MapPin className="h-3.5 w-3.5" /> {p.location || "Remote"} · Apply by {formatDate(p.lastDateToApply)}
                    </div>
                    {p.eligibility && <p className="mt-1 text-xs text-muted-foreground">Eligibility: {p.eligibility}</p>}
                  </div>
                  <Button size="sm" variant="outline">View details</Button>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
