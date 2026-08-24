"use client";

import { useState } from "react";
import { Upload } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { assignmentsApi } from "@/lib/api/assignments";
import { extractErrorMessage } from "@/lib/api/client";
import { formatDate } from "@/lib/utils";
import { Assignment } from "@/types";
import { ClipboardList } from "lucide-react";

export default function StudentAssignmentsPage() {
  const { data, isLoading, error, refetch } = useFetch(() => assignmentsApi.list({ page: 0, size: 20 }), []);
  const [active, setActive] = useState<Assignment | null>(null);
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    if (!active) return;
    setSubmitting(true);
    try {
      await assignmentsApi.submit(active.id, { text });
      toast.success("Assignment submitted");
      setActive(null);
      setText("");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <PageHeader title="Assignments" description="Track due dates and submit your work on time." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={ClipboardList} title="No assignments yet" description="New assignments from your courses will show up here." />
      ) : (
        <div className="space-y-3">
          {data.content.map((a) => (
            <Card key={a.id}>
              <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-medium">{a.title}</p>
                  <p className="text-sm text-muted-foreground">{a.courseTitle} · Due {formatDate(a.dueDate)}</p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={a.status} />
                  {a.status === "NOT_SUBMITTED" && (
                    <Button size="sm" className="gap-2" onClick={() => setActive(a)}>
                      <Upload className="h-4 w-4" /> Submit
                    </Button>
                  )}
                  {a.score !== undefined && <span className="text-sm font-medium">{a.score}/{a.maxScore}</span>}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={!!active} onOpenChange={(open) => !open && setActive(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Submit assignment</DialogTitle>
            <DialogDescription>{active?.title}</DialogDescription>
          </DialogHeader>
          <Textarea
            rows={6}
            placeholder="Paste your answer, a link to your repo, or notes for the reviewer..."
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setActive(null)}>Cancel</Button>
            <Button onClick={handleSubmit} disabled={submitting || !text.trim()}>
              {submitting ? "Submitting..." : "Submit assignment"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
