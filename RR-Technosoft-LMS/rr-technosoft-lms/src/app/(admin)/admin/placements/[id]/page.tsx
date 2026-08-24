"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Briefcase, MapPin, Pencil, CalendarClock, Users } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { extractErrorMessage } from "@/lib/api/client";
import { formatDate } from "@/lib/utils";
import { PlacementApplication, PlacementStatus } from "@/types";
import { ApplicationStatusSelect } from "@/components/placements/application-status-select";
import { ScheduleInterviewDialog } from "@/components/placements/schedule-interview-dialog";
import { InterviewsDialog } from "@/components/placements/interviews-dialog";

const STATUS_OPTIONS: PlacementStatus[] = ["DRAFT", "OPEN", "CLOSED", "COMPLETED", "CANCELLED"];

export default function AdminPlacementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: placement, isLoading, error, refetch } = useFetch(() => placementsApi.get(id), [id]);
  const { data: applications, refetch: refetchApplications } = useFetch(
    () => placementsApi.listApplications(id, { page: 0, size: 50 }),
    [id]
  );

  const [scheduleFor, setScheduleFor] = useState<string | null>(null);
  const [interviewsFor, setInterviewsFor] = useState<string | null>(null);

  async function handleStatusChange(status: string) {
    try {
      await placementsApi.setStatus(id, status as PlacementStatus);
      toast.success("Drive status updated");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  const columns: Column<PlacementApplication>[] = [
    {
      key: "student",
      header: "Student",
      render: (a) => (
        <div>
          <p className="font-medium">{a.studentName}</p>
          {a.studentIdNumber && <p className="text-xs text-muted-foreground">{a.studentIdNumber}</p>}
        </div>
      ),
    },
    { key: "applied", header: "Applied on", render: (a) => formatDate(a.appliedAt) },
    { key: "resume", header: "Resume", render: (a) => (a.resumeUrl ? <a href={a.resumeUrl} target="_blank" rel="noreferrer" className="text-primary underline">View</a> : "—") },
    {
      key: "status",
      header: "Status",
      render: (a) => <ApplicationStatusSelect applicationId={a.id} status={a.status} onChanged={refetchApplications} />,
    },
    {
      key: "actions",
      header: "",
      render: (a) => (
        <Button size="sm" variant="outline" className="gap-1.5" onClick={() => setInterviewsFor(a.id)}>
          <CalendarClock className="h-3.5 w-3.5" /> Interviews
        </Button>
      ),
      className: "text-right",
    },
  ];

  if (error) return <ErrorState message={error} onRetry={refetch} />;

  return (
    <div>
      <PageHeader
        title={isLoading ? "Loading..." : `${placement?.companyName} — ${placement?.role}`}
        description="Job drive details, applications and interview tracking."
        actions={
          placement && (
            <Button asChild variant="outline" className="gap-2">
              <Link href={`/admin/placements/${id}/edit`}><Pencil className="h-4 w-4" /> Edit</Link>
            </Button>
          )
        }
      />

      {placement && (
        <Card className="mb-6">
          <CardContent className="grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center gap-2 text-sm">
              <MapPin className="h-4 w-4 text-muted-foreground" /> {placement.location || "Remote / unspecified"}
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Briefcase className="h-4 w-4 text-muted-foreground" /> {placement.packageLpa ?? "Not disclosed"}
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Users className="h-4 w-4 text-muted-foreground" /> {placement.applicantsCount ?? 0} applicants
            </div>
            <div>
              <Select value={placement.status} onValueChange={handleStatusChange}>
                <SelectTrigger className="h-8"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>

            {placement.description && (
              <p className="text-sm text-muted-foreground sm:col-span-2 lg:col-span-4">{placement.description}</p>
            )}
            {placement.eligibility && (
              <p className="text-sm sm:col-span-2 lg:col-span-4"><span className="font-medium">Eligibility: </span>{placement.eligibility}</p>
            )}
            {placement.skillsRequired && placement.skillsRequired.length > 0 && (
              <div className="flex flex-wrap gap-1.5 sm:col-span-2 lg:col-span-4">
                {placement.skillsRequired.map((s) => <StatusBadge key={s} status={s} />)}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <h2 className="mb-3 font-display text-lg font-semibold">Applications</h2>
      <DataTable
        columns={columns}
        data={applications?.content ?? []}
        rowKey={(a) => a.id}
        emptyTitle="No applications yet"
        emptyDescription="Applications will appear here as students apply."
      />

      <ScheduleInterviewDialog
        open={!!scheduleFor}
        onOpenChange={(open) => !open && setScheduleFor(null)}
        applicationId={scheduleFor}
        onScheduled={() => {
          setScheduleFor(null);
          refetchApplications();
        }}
      />

      <InterviewsDialog
        open={!!interviewsFor}
        onOpenChange={(open) => !open && setInterviewsFor(null)}
        applicationId={interviewsFor}
        onScheduleNew={() => {
          setScheduleFor(interviewsFor);
          setInterviewsFor(null);
        }}
      />
    </div>
  );
}
