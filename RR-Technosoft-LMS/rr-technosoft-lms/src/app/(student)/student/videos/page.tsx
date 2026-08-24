"use client";

import { useState } from "react";
import { Video as VideoIcon, Play, X } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { videosApi } from "@/lib/api/videos";
import { VideoResource } from "@/types";

export default function StudentVideosPage() {
  const { data, isLoading, error, refetch } = useFetch(() => videosApi.list({ page: 0, size: 50 }), []);
  const [playing, setPlaying] = useState<VideoResource | null>(null);

  async function handlePlay(video: VideoResource) {
    setPlaying(video);
    try {
      await videosApi.recordView(video.id);
    } catch {
      // View-count tracking is best-effort — playback isn't blocked on it.
    }
  }

  return (
    <div>
      <PageHeader title="Video Library" description="Watch instructional videos to support your coursework." />

      {playing && (
        <Card className="mb-6">
          <CardContent className="p-4">
            <div className="mb-3 flex items-center justify-between">
              <p className="font-display font-semibold">{playing.title}</p>
              <button onClick={() => setPlaying(null)} className="text-muted-foreground hover:text-foreground">
                <X className="h-4 w-4" />
              </button>
            </div>
            {playing.source === "UPLOAD" ? (
              <video src={playing.videoUrl} controls className="aspect-video w-full rounded-md bg-black" />
            ) : (
              <a href={playing.videoUrl} target="_blank" rel="noreferrer" className="text-primary underline">
                Open video in a new tab
              </a>
            )}
          </CardContent>
        </Card>
      )}

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-40 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={VideoIcon} title="No videos yet" description="Check back soon — new videos will show up here." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((v) => (
            <Card key={v.id} className="cursor-pointer transition-colors hover:border-primary" onClick={() => handlePlay(v)}>
              <CardContent className="p-0">
                <div className="flex aspect-video items-center justify-center rounded-t-lg bg-muted">
                  {v.thumbnailUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={v.thumbnailUrl} alt={v.title} className="h-full w-full rounded-t-lg object-cover" />
                  ) : (
                    <Play className="h-8 w-8 text-muted-foreground" />
                  )}
                </div>
                <div className="space-y-1.5 p-4">
                  <div className="flex items-center gap-2">
                    <p className="font-display font-semibold">{v.title}</p>
                    {v.category && <Badge variant="outline">{v.category}</Badge>}
                  </div>
                  {v.description && <p className="line-clamp-2 text-sm text-muted-foreground">{v.description}</p>}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
