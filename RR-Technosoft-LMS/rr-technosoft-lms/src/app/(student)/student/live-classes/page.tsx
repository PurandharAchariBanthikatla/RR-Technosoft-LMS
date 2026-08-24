"use client";

import { Video, ExternalLink, PlayCircle } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { liveClassesApi } from "@/lib/api/live-classes";
import { formatDate, formatTime } from "@/lib/utils";

export default function StudentLiveClassesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => liveClassesApi.list({ page: 0, size: 20 }), []);

  return (
    <div>
      <PageHeader title="Live Classes" description="Join live sessions or catch up with recordings." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={Video} title="No classes scheduled" description="Upcoming live sessions will appear here." />
      ) : (
        <div className="space-y-3">
          {data.content.map((c) => (
            <Card key={c.id}>
              <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary">
                    <Video className="h-5 w-5" />
                  </span>
                  <div>
                    <p className="font-medium">{c.title}</p>
                    <p className="text-sm text-muted-foreground">
                      {c.courseTitle} · {formatDate(c.startTime)} · {formatTime(c.startTime)} – {formatTime(c.endTime)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={c.status} />
                  {c.status === "LIVE" && c.meetingUrl && (
                    <Button size="sm" asChild className="gap-2">
                      <a href={c.meetingUrl} target="_blank" rel="noreferrer"><ExternalLink className="h-4 w-4" /> Join now</a>
                    </Button>
                  )}
                  {c.status === "ENDED" && c.recordingUrl && (
                    <Button size="sm" variant="outline" asChild className="gap-2">
                      <a href={c.recordingUrl} target="_blank" rel="noreferrer"><PlayCircle className="h-4 w-4" /> Recording</a>
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
