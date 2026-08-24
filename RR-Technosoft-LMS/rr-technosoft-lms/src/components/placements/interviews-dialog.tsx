"use client";

import { Plus } from "lucide-react";

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { formatDate, formatTime } from "@/lib/utils";
import { CalendarClock } from "lucide-react";

interface InterviewsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  applicationId: string | null;
  onScheduleNew: () => void;
}

export function InterviewsDialog({ open, onOpenChange, applicationId, onScheduleNew }: InterviewsDialogProps) {
  const { data, isLoading } = useFetch(
    () => (applicationId ? placementsApi.listInterviews(applicationId) : Promise.resolve([])),
    [applicationId, open]
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Interview rounds</DialogTitle>
        </DialogHeader>

        <div className="space-y-3">
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Loading...</p>
          ) : !data || data.length === 0 ? (
            <EmptyState icon={CalendarClock} title="No interviews scheduled" description="Schedule the first round for this application." />
          ) : (
            data.map((i) => (
              <div key={i.id} className="rounded-lg border p-3">
                <div className="flex items-center justify-between">
                  <p className="font-medium">Round {i.roundNumber}: {i.roundName}</p>
                  <StatusBadge status={i.status} />
                </div>
                <p className="mt-1 text-sm text-muted-foreground">
                  {formatDate(i.scheduledAt)} at {formatTime(i.scheduledAt)} · {i.mode}
                </p>
                {i.interviewerName && <p className="text-xs text-muted-foreground">Interviewer: {i.interviewerName}</p>}
                {i.venueOrLink && <p className="text-xs text-muted-foreground">{i.venueOrLink}</p>}
                <div className="mt-2 flex items-center gap-2">
                  <StatusBadge status={i.result} />
                  {i.feedback && <p className="text-xs text-muted-foreground">{i.feedback}</p>}
                </div>
              </div>
            ))
          )}

          <Button variant="outline" className="w-full gap-2" onClick={onScheduleNew}>
            <Plus className="h-4 w-4" /> Schedule another round
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
