"use client";

import { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ShieldAlert, Loader2, PlayCircle } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { DataTable, type Column } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { backupApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { formatDate, formatTime } from "@/lib/utils";
import { backupConfigSchema, type BackupConfigInput } from "@/lib/validations/administration";
import { BackupRun } from "@/types";

export default function BackupPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data: config, isLoading, error, refetch } = useFetch(() => backupApi.getConfig(), []);
  const { data: runsPage, isLoading: runsLoading, refetch: refetchRuns } = useFetch(
    () => backupApi.listRuns({ page: 0, size: 10 }),
    []
  );
  const [triggering, setTriggering] = useState(false);

  const { register, control, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<BackupConfigInput>({ resolver: zodResolver(backupConfigSchema) });

  useEffect(() => {
    if (config) reset(config);
  }, [config, reset]);

  async function onSubmit(values: BackupConfigInput) {
    try {
      await backupApi.updateConfig(values);
      toast.success("Backup configuration updated");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function handleTrigger() {
    setTriggering(true);
    try {
      await backupApi.trigger();
      toast.success("Backup started — check history for progress");
      refetchRuns();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setTriggering(false);
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can manage backups." />;
  }

  const columns: Column<BackupRun>[] = [
    { key: "startedAt", header: "Started", render: (r) => `${formatDate(r.startedAt)} ${formatTime(r.startedAt)}` },
    { key: "status", header: "Status", render: (r) => <StatusBadge status={r.status} /> },
    { key: "size", header: "Size", render: (r) => (r.sizeMb ? `${r.sizeMb} MB` : "—") },
    { key: "trigger", header: "Trigger", render: (r) => (r.triggeredBy ? "Manual" : "Scheduled") },
    { key: "error", header: "Notes", render: (r) => r.errorMessage ?? "—" },
  ];

  return (
    <div>
      <PageHeader
        title="Backup & Restore"
        description="Configure automatic backups and trigger a run on demand."
        actions={
          <Button onClick={handleTrigger} disabled={triggering} className="gap-2">
            {triggering ? <Loader2 className="h-4 w-4 animate-spin" /> : <PlayCircle className="h-4 w-4" />}
            Run Backup Now
          </Button>
        }
      />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-72 w-full" />}

      {config && !error && (
        <Card className="mb-6">
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit(onSubmit)} className="grid gap-5 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="scheduleCron">Schedule (cron)</Label>
                <Input id="scheduleCron" placeholder="0 0 2 * * *" {...register("scheduleCron")} />
                {errors.scheduleCron && <p className="text-xs text-destructive">{errors.scheduleCron.message}</p>}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="retentionDays">Retention (days)</Label>
                <Input id="retentionDays" type="number" {...register("retentionDays", { valueAsNumber: true })} />
              </div>
              <div className="space-y-1.5">
                <Label>Storage Type</Label>
                <Controller
                  name="storageType"
                  control={control}
                  render={({ field: { value, onChange } }) => (
                    <Select value={value} onValueChange={onChange}>
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="LOCAL">LOCAL</SelectItem>
                        <SelectItem value="S3">S3</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="storageLocation">Storage Location</Label>
                <Input id="storageLocation" placeholder="/var/backups/rr-lms or s3://bucket/path" {...register("storageLocation")} />
                {errors.storageLocation && <p className="text-xs text-destructive">{errors.storageLocation.message}</p>}
              </div>
              <div className="flex items-center justify-between sm:col-span-2">
                <Label htmlFor="autoBackupEnabled">Enable automatic backups</Label>
                <Controller
                  name="autoBackupEnabled"
                  control={control}
                  render={({ field: { value, onChange } }) => <Switch id="autoBackupEnabled" checked={value} onCheckedChange={onChange} />}
                />
              </div>
              <div className="sm:col-span-2 flex justify-end">
                <Button type="submit" disabled={isSubmitting} className="gap-2">
                  {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  Save Configuration
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <h3 className="mb-3 font-display text-sm font-semibold uppercase tracking-wide text-muted-foreground">Backup History</h3>
      <DataTable
        columns={columns}
        data={runsPage?.content ?? []}
        isLoading={runsLoading}
        rowKey={(r) => r.id}
        emptyTitle="No backups yet"
        emptyDescription="Run a backup or wait for the next scheduled run."
      />
    </div>
  );
}
