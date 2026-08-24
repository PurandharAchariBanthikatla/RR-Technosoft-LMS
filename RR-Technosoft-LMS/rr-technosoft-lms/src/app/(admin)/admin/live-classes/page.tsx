"use client";

import { Plus, Video } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { liveClassesApi } from "@/lib/api/live-classes";
import { formatDate, formatTime } from "@/lib/utils";
import { LiveClass } from "@/types";

export default function AdminLiveClassesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => liveClassesApi.list({ page: 0, size: 20 }), []);

  const columns: Column<LiveClass>[] = [
    {
      key: "title",
      header: "Session",
      render: (c) => (
        <div className="flex items-center gap-2">
          <Video className="h-4 w-4 text-primary" />
          <div>
            <p className="font-medium">{c.title}</p>
            <p className="text-xs text-muted-foreground">{c.courseTitle}</p>
          </div>
        </div>
      ),
    },
    { key: "instructor", header: "Instructor", render: (c) => c.instructorName },
    { key: "when", header: "Schedule", render: (c) => `${formatDate(c.startTime)} · ${formatTime(c.startTime)} – ${formatTime(c.endTime)}` },
    { key: "status", header: "Status", render: (c) => <StatusBadge status={c.status} /> },
  ];

  return (
    <div>
      <PageHeader
        title="Live Classes"
        description="Schedule and manage live sessions for your cohorts."
        actions={<Button className="gap-2"><Plus className="h-4 w-4" /> Schedule class</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(c) => c.id} emptyTitle="No live classes scheduled" />
      )}
    </div>
  );
}
