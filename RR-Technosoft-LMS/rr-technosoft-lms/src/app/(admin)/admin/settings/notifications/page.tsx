"use client";

import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ShieldAlert, Loader2 } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { notificationSettingsApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { notificationSettingsSchema, type NotificationSettingsInput } from "@/lib/validations/administration";

export default function NotificationSettingsPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => notificationSettingsApi.get(), []);

  const { register, control, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<NotificationSettingsInput>({ resolver: zodResolver(notificationSettingsSchema) });

  useEffect(() => {
    if (data) {
      reset({
        smtpHost: data.smtpHost ?? "",
        smtpPort: data.smtpPort,
        smtpUsername: data.smtpUsername ?? "",
        smtpPassword: "",
        smtpUseTls: data.smtpUseTls,
        fromName: data.fromName,
        fromEmail: data.fromEmail ?? "",
        emailNotificationsEnabled: data.emailNotificationsEnabled,
        smsNotificationsEnabled: data.smsNotificationsEnabled,
        pushNotificationsEnabled: data.pushNotificationsEnabled,
        digestFrequency: data.digestFrequency,
      });
    }
  }, [data, reset]);

  async function onSubmit(values: NotificationSettingsInput) {
    try {
      await notificationSettingsApi.update(values);
      toast.success("Notification settings updated");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can edit notification settings." />;
  }

  return (
    <div>
      <PageHeader title="Notifications & Email" description="SMTP configuration and default notification channel preferences." />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-96 w-full" />}

      {data && !error && (
        <Card>
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
              <div>
                <h3 className="mb-4 font-display text-sm font-semibold uppercase tracking-wide text-muted-foreground">SMTP</h3>
                <div className="grid gap-5 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="smtpHost">SMTP Host</Label>
                    <Input id="smtpHost" {...register("smtpHost")} />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="smtpPort">SMTP Port</Label>
                    <Input id="smtpPort" type="number" {...register("smtpPort", { valueAsNumber: true })} />
                    {errors.smtpPort && <p className="text-xs text-destructive">{errors.smtpPort.message}</p>}
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="smtpUsername">SMTP Username</Label>
                    <Input id="smtpUsername" {...register("smtpUsername")} />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="smtpPassword">
                      SMTP Password {data.smtpConfigured && <span className="text-xs text-muted-foreground">(configured — leave blank to keep)</span>}
                    </Label>
                    <Input id="smtpPassword" type="password" placeholder={data.smtpConfigured ? "••••••••" : ""} {...register("smtpPassword")} />
                  </div>
                  <div className="flex items-center justify-between sm:col-span-2">
                    <Label htmlFor="smtpUseTls">Use TLS</Label>
                    <Controller
                      name="smtpUseTls"
                      control={control}
                      render={({ field: { value, onChange } }) => <Switch id="smtpUseTls" checked={value} onCheckedChange={onChange} />}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="fromName">From Name</Label>
                    <Input id="fromName" {...register("fromName")} />
                    {errors.fromName && <p className="text-xs text-destructive">{errors.fromName.message}</p>}
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="fromEmail">From Email</Label>
                    <Input id="fromEmail" {...register("fromEmail")} />
                    {errors.fromEmail && <p className="text-xs text-destructive">{errors.fromEmail.message}</p>}
                  </div>
                </div>
              </div>

              <div className="border-t pt-6">
                <h3 className="mb-4 font-display text-sm font-semibold uppercase tracking-wide text-muted-foreground">Channels</h3>
                <div className="space-y-4">
                  {([
                    ["emailNotificationsEnabled", "Email notifications"],
                    ["smsNotificationsEnabled", "SMS notifications"],
                    ["pushNotificationsEnabled", "Push notifications"],
                  ] as const).map(([name, label]) => (
                    <div key={name} className="flex items-center justify-between">
                      <Label htmlFor={name}>{label}</Label>
                      <Controller
                        name={name}
                        control={control}
                        render={({ field: { value, onChange } }) => <Switch id={name} checked={value} onCheckedChange={onChange} />}
                      />
                    </div>
                  ))}
                  <div className="space-y-1.5 pt-2">
                    <Label>Digest Frequency</Label>
                    <Controller
                      name="digestFrequency"
                      control={control}
                      render={({ field: { value, onChange } }) => (
                        <Select value={value} onValueChange={onChange}>
                          <SelectTrigger className="w-56"><SelectValue /></SelectTrigger>
                          <SelectContent>
                            {["INSTANT", "DAILY", "WEEKLY", "NONE"].map((v) => (
                              <SelectItem key={v} value={v}>{v}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </div>
                </div>
              </div>

              <div className="flex justify-end">
                <Button type="submit" disabled={isSubmitting} className="gap-2">
                  {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  Save Changes
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
