"use client";

import { FileText, Link as LinkIcon, Download } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { learningResourcesApi } from "@/lib/api/learning-resources";
import { extractErrorMessage } from "@/lib/api/client";

export default function StudentLearningResourcesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => learningResourcesApi.list({ page: 0, size: 50 }), []);

  async function handleOpen(id: string, url: string) {
    try {
      await learningResourcesApi.recordDownload(id);
    } catch (err) {
      // Non-fatal — the download itself still proceeds even if the count fails to record.
      toast.error(extractErrorMessage(err));
    } finally {
      window.open(url, "_blank");
    }
  }

  return (
    <div>
      <PageHeader title="Learning Resources" description="Reference material and downloads to support your coursework." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-32 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={FileText} title="No resources yet" description="Check back soon — new resources will show up here." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((r) => {
            const url = r.fileUrl ?? r.externalUrl;
            return (
              <Card key={r.id}>
                <CardContent className="flex h-full flex-col gap-3 p-5">
                  <div className="flex items-center gap-2">
                    {r.resourceType === "LINK" ? <LinkIcon className="h-5 w-5 text-primary" /> : <FileText className="h-5 w-5 text-primary" />}
                    <Badge variant="outline">{r.resourceType}</Badge>
                  </div>
                  <div>
                    <p className="font-display font-semibold">{r.title}</p>
                    {r.description && <p className="mt-1 text-sm text-muted-foreground">{r.description}</p>}
                  </div>
                  {url && (
                    <Button size="sm" className="mt-auto gap-1.5 self-start" onClick={() => handleOpen(r.id, url)}>
                      <Download className="h-3.5 w-3.5" /> {r.resourceType === "LINK" ? "Open link" : "Download"}
                    </Button>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
