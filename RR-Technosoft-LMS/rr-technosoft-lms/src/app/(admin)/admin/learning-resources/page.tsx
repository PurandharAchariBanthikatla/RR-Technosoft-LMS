"use client";

import { useState } from "react";
import { FileText, Plus, Pencil, Trash2, Download, Link as LinkIcon } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useFetch } from "@/hooks/use-fetch";
import { learningResourcesApi } from "@/lib/api/learning-resources";
import { extractErrorMessage } from "@/lib/api/client";
import { LearningResource } from "@/types";
import { ResourceFormDialog } from "@/components/learning/resource-form-dialog";

export default function AdminLearningResourcesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => learningResourcesApi.list({ page: 0, size: 50 }), []);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<LearningResource | undefined>(undefined);
  const [deleting, setDeleting] = useState<LearningResource | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleDelete() {
    if (!deleting) return;
    setBusy(true);
    try {
      await learningResourcesApi.remove(deleting.id);
      toast.success("Resource deleted");
      setDeleting(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const columns: Column<LearningResource>[] = [
    {
      key: "title",
      header: "Resource",
      render: (r) => (
        <div className="flex items-center gap-2">
          {r.resourceType === "LINK" ? <LinkIcon className="h-4 w-4 text-primary" /> : <FileText className="h-4 w-4 text-primary" />}
          <div>
            <p className="font-medium">{r.title}</p>
            {r.category && <p className="text-xs text-muted-foreground">{r.category}</p>}
          </div>
        </div>
      ),
    },
    { key: "type", header: "Type", render: (r) => <Badge variant="outline">{r.resourceType}</Badge> },
    { key: "downloads", header: "Downloads", render: (r) => r.downloadCount },
    {
      key: "status",
      header: "Status",
      render: (r) => <Badge variant={r.isPublished ? "success" : "outline"}>{r.isPublished ? "Published" : "Draft"}</Badge>,
    },
    {
      key: "actions",
      header: "",
      render: (r) => (
        <div className="flex justify-end gap-2">
          {(r.fileUrl || r.externalUrl) && (
            <Button size="icon" variant="ghost" asChild>
              <a href={r.fileUrl ?? r.externalUrl} target="_blank" rel="noreferrer"><Download className="h-4 w-4" /></a>
            </Button>
          )}
          <Button size="icon" variant="ghost" onClick={() => { setEditing(r); setFormOpen(true); }}>
            <Pencil className="h-4 w-4" />
          </Button>
          <Button size="icon" variant="ghost" onClick={() => setDeleting(r)}>
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
        title="Learning Resources"
        description="Manage downloadable resources and reference links for students."
        actions={
          <Button className="gap-2" onClick={() => { setEditing(undefined); setFormOpen(true); }}>
            <Plus className="h-4 w-4" /> New resource
          </Button>
        }
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(r) => r.id} emptyTitle="No resources yet" />
      )}

      <ResourceFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        resource={editing}
        onSaved={() => { setFormOpen(false); refetch(); }}
      />

      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete resource?"
        description={`"${deleting?.title}" will be permanently removed.`}
        confirmLabel="Delete"
        destructive
        loading={busy}
        onConfirm={handleDelete}
      />
    </div>
  );
}
