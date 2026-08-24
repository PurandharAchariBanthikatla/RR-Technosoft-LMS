"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { MapPin, Briefcase, CalendarClock, Loader2, Upload } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { extractErrorMessage } from "@/lib/api/client";
import { formatDate, formatTime } from "@/lib/utils";

export default function StudentPlacementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: placement, isLoading, error, refetch } = useFetch(() => placementsApi.get(id), [id]);
  const { data: myApplications, refetch: refetchApplications } = useFetch(() => placementsApi.myApplications({ size: 100 }), []);
  const { data: myInterviews } = useFetch(() => placementsApi.myInterviews(), []);
  const [applying, setApplying] = useState(false);
  const [resumeFile, setResumeFile] = useState<File | null>(null);
  const [uploadingResume, setUploadingResume] = useState(false);

  const existingApplication = myApplications?.content.find((a) => a.placementId === id);
  const interviewsForThisDrive = (myInterviews ?? []).filter((i) => i.placementId === id);

  async function handleApply() {
    setApplying(true);
    try {
      await placementsApi.apply(id);
      toast.success("Application submitted");
      refetchApplications();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setApplying(false);
    }
  }

  async function handleWithdraw() {
    if (!existingApplication) return;
    setApplying(true);
    try {
      await placementsApi.withdraw(existingApplication.id);
      toast.success("Application withdrawn");
      refetchApplications();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setApplying(false);
    }
  }

  async function handleUploadResume() {
    if (!existingApplication || !resumeFile) return;
    setUploadingResume(true);
    try {
      await placementsApi.attachResume(existingApplication.id, resumeFile);
      toast.success("Resume uploaded");
      setResumeFile(null);
      refetchApplications();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setUploadingResume(false);
    }
  }

  if (error) return <ErrorState message={error} onRetry={refetch} />;
  if (isLoading || !placement) return <p className="text-sm text-muted-foreground">Loading...</p>;

  return (
    <div>
      <PageHeader title={`${placement.companyName} — ${placement.role}`} description="Job drive details and your application status." />

      <Card className="mb-6">
        <CardContent className="space-y-4 p-5">
          <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" /> {placement.location || "Remote"}</span>
            <span className="flex items-center gap-1.5"><Briefcase className="h-4 w-4" /> {placement.packageLpa ?? "Not disclosed"}</span>
            <span>Apply by {formatDate(placement.lastDateToApply)}</span>
            <StatusBadge status={placement.status} />
          </div>

          {placement.description && <p className="text-sm">{placement.description}</p>}
          {placement.eligibility && <p className="text-sm"><span className="font-medium">Eligibility: </span>{placement.eligibility}</p>}
          {placement.skillsRequired && placement.skillsRequired.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {placement.skillsRequired.map((s) => <StatusBadge key={s} status={s} />)}
            </div>
          )}

          <div className="border-t pt-4">
            {existingApplication ? (
              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="text-sm">Your application status:</span>
                  <StatusBadge status={existingApplication.status} />
                  {existingApplication.status !== "WITHDRAWN" && existingApplication.status !== "SELECTED" && existingApplication.status !== "REJECTED" && (
                    <Button size="sm" variant="outline" onClick={handleWithdraw} disabled={applying}>
                      {applying && <Loader2 className="h-4 w-4 animate-spin" />} Withdraw
                    </Button>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm text-muted-foreground">
                    Resume: {existingApplication.resumeUrl ? "Uploaded" : "Not uploaded yet"}
                  </span>
                  <Input
                    type="file"
                    accept=".pdf,.doc,.docx"
                    className="h-9 max-w-xs"
                    onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)}
                  />
                  <Button size="sm" onClick={handleUploadResume} disabled={!resumeFile || uploadingResume} className="gap-1.5">
                    {uploadingResume ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
                    {existingApplication.resumeUrl ? "Replace resume" : "Upload resume"}
                  </Button>
                </div>
              </div>
            ) : placement.status === "OPEN" ? (
              <Button onClick={handleApply} disabled={applying}>
                {applying && <Loader2 className="h-4 w-4 animate-spin" />} Apply now
              </Button>
            ) : (
              <p className="text-sm text-muted-foreground">This drive is not currently accepting applications.</p>
            )}
          </div>
        </CardContent>
      </Card>

      {interviewsForThisDrive.length > 0 && (
        <>
          <h2 className="mb-3 font-display text-lg font-semibold">Your interview schedule</h2>
          <div className="space-y-3">
            {interviewsForThisDrive.map((i) => (
              <Card key={i.id}>
                <CardContent className="flex items-center justify-between p-4">
                  <div>
                    <p className="flex items-center gap-1.5 font-medium"><CalendarClock className="h-4 w-4 text-primary" /> Round {i.roundNumber}: {i.roundName}</p>
                    <p className="text-sm text-muted-foreground">{formatDate(i.scheduledAt)} at {formatTime(i.scheduledAt)} · {i.mode}</p>
                    {i.venueOrLink && <p className="text-xs text-muted-foreground">{i.venueOrLink}</p>}
                  </div>
                  <StatusBadge status={i.status} />
                </CardContent>
              </Card>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
