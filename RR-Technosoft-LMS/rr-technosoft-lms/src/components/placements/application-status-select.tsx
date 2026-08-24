"use client";

import { useState } from "react";
import { toast } from "sonner";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { placementsApi } from "@/lib/api/placements";
import { extractErrorMessage } from "@/lib/api/client";
import { ApplicationStatus } from "@/types";

const OPTIONS: ApplicationStatus[] = ["APPLIED", "SHORTLISTED", "INTERVIEW_SCHEDULED", "SELECTED", "REJECTED", "WITHDRAWN"];

interface ApplicationStatusSelectProps {
  applicationId: string;
  status: ApplicationStatus;
  onChanged: () => void;
}

export function ApplicationStatusSelect({ applicationId, status, onChanged }: ApplicationStatusSelectProps) {
  const [updating, setUpdating] = useState(false);

  async function handleChange(next: string) {
    setUpdating(true);
    try {
      await placementsApi.updateApplicationStatus(applicationId, next as ApplicationStatus);
      toast.success("Application status updated");
      onChanged();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setUpdating(false);
    }
  }

  return (
    <Select value={status} onValueChange={handleChange} disabled={updating}>
      <SelectTrigger className="h-8 w-[180px]"><SelectValue /></SelectTrigger>
      <SelectContent>
        {OPTIONS.map((opt) => (
          <SelectItem key={opt} value={opt}>{opt.replace(/_/g, " ")}</SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
