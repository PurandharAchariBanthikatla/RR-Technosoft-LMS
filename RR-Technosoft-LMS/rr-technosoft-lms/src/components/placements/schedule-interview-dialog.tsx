"use client";

import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { placementsApi } from "@/lib/api/placements";
import { extractErrorMessage } from "@/lib/api/client";
import { InterviewMode } from "@/types";

interface ScheduleInterviewDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  applicationId: string | null;
  onScheduled: () => void;
}

interface FormValues {
  roundName: string;
  scheduledAt: string;
  mode: InterviewMode;
  venueOrLink: string;
  interviewerName: string;
}

export function ScheduleInterviewDialog({ open, onOpenChange, applicationId, onScheduled }: ScheduleInterviewDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FormValues>({
    defaultValues: { roundName: "", scheduledAt: "", mode: "ONLINE", venueOrLink: "", interviewerName: "" },
  });

  async function onSubmit(values: FormValues) {
    if (!applicationId) return;
    setSubmitting(true);
    try {
      await placementsApi.scheduleInterview(applicationId, {
        roundName: values.roundName,
        scheduledAt: new Date(values.scheduledAt).toISOString(),
        mode: values.mode,
        venueOrLink: values.venueOrLink || undefined,
        interviewerName: values.interviewerName || undefined,
      });
      toast.success("Interview scheduled");
      reset();
      onScheduled();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Schedule interview round</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="roundName">Round name</Label>
            <Input id="roundName" placeholder="Technical Round 1" {...register("roundName", { required: true })} />
            {errors.roundName && <p className="text-xs text-destructive">Round name is required</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="scheduledAt">Date &amp; time</Label>
            <Input id="scheduledAt" type="datetime-local" {...register("scheduledAt", { required: true })} />
            {errors.scheduledAt && <p className="text-xs text-destructive">Date &amp; time is required</p>}
          </div>

          <div className="space-y-1.5">
            <Label>Mode</Label>
            <Controller
              control={control}
              name="mode"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ONLINE">Online</SelectItem>
                    <SelectItem value="OFFLINE">Offline</SelectItem>
                    <SelectItem value="TELEPHONIC">Telephonic</SelectItem>
                  </SelectContent>
                </Select>
              )}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="venueOrLink">Venue / meeting link</Label>
            <Input id="venueOrLink" placeholder="https://meet.google.com/..." {...register("venueOrLink")} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="interviewerName">Interviewer</Label>
            <Input id="interviewerName" {...register("interviewerName")} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              Schedule
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
