"use client";

import { useState } from "react";
import { Video as VideoIcon, Plus, Pencil, Trash2, Play } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useFetch } from "@/hooks/use-fetch";
import { videosApi } from "@/lib/api/videos";
import { extractErrorMessage } from "@/lib/api/client";
import { VideoResource } from "@/types";
import { VideoFormDialog } from "@/components/videos/video-form-dialog";

export default function AdminVideosPage() {
  const { data, isLoading, error, refetch } = useFetch(() => videosApi.list({ page: 0, size: 50 }), []);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<VideoResource | undefined>(undefined);
  const [deleting, setDeleting] = useState<VideoResource | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleDelete() {
    if (!deleting) return;
    setBusy(true);
    try {
      await videosApi.remove(deleting.id);
      toast.success("Video deleted");
      setDeleting(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const columns: Column<VideoResource>[] = [
    {
      key: "title",
      header: "Video",
      render: (v) => (
        <div className="flex items-center gap-2">
          <VideoIcon className="h-4 w-4 text-primary" />
          <div>
            <p className="font-medium">{v.title}</p>
            {v.category && <p className="text-xs text-muted-foreground">{v.category}</p>}
          </div>
        </div>
      ),
    },
    { key: "source", header: "Source", render: (v) => <Badge variant="outline">{v.source}</Badge> },
    { key: "views", header: "Views", render: (v) => v.viewCount },
    {
      key: "status",
      header: "Status",
      render: (v) => <Badge variant={v.isPublished ? "success" : "outline"}>{v.isPublished ? "Published" : "Draft"}</Badge>,
    },
    {
      key: "actions",
      header: "",
      render: (v) => (
        <div className="flex justify-end gap-2">
          {v.videoUrl && (
            <Button size="icon" variant="ghost" asChild>
              <a href={v.videoUrl} target="_blank" rel="noreferrer"><Play className="h-4 w-4" /></a>
            </Button>
          )}
          <Button size="icon" variant="ghost" onClick={() => { setEditing(v); setFormOpen(true); }}>
            <Pencil className="h-4 w-4" />
          </Button>
          <Button size="icon" variant="ghost" onClick={() => setDeleting(v)}>
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ),
      className: "text-right",
    },
  ];

  return (
    <div>
      <PageHeader
        title="Video Library"
        description="Manage instructional videos available to students."
        actions={
          <Button className="gap-2" onClick={() => { setEditing(undefined); setFormOpen(true); }}>
            <Plus className="h-4 w-4" /> New video
          </Button>
        }
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(v) => v.id} emptyTitle="No videos yet" />
      )}

      <VideoFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        video={editing}
        onSaved={() => { setFormOpen(false); refetch(); }}
      />

      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete video?"
        description={`"${deleting?.title}" will be permanently removed.`}
        confirmLabel="Delete"
        destructive
        loading={busy}
        onConfirm={handleDelete}
      />
    </div>
  );
}
