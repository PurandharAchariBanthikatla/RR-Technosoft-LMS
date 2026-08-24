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
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { securitySettingsApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { securitySettingsSchema, type SecuritySettingsInput } from "@/lib/validations/administration";

const boolFields: { name: keyof SecuritySettingsInput; label: string; hint?: string }[] = [
  { name: "passwordRequireUppercase", label: "Require an uppercase letter" },
  { name: "passwordRequireNumber", label: "Require a number" },
  { name: "passwordRequireSpecialChar", label: "Require a special character" },
  { name: "mfaRequiredForAdmins", label: "Require MFA for Admin accounts", hint: "Applies to ADMIN and SUPER_ADMIN logins" },
  { name: "forceLogoutOnPasswordChange", label: "Force logout on password change", hint: "Revokes all other active sessions" },
];

const numberFields: { name: keyof SecuritySettingsInput; label: string; hint?: string }[] = [
  { name: "passwordMinLength", label: "Minimum password length" },
  { name: "passwordExpiryDays", label: "Password expiry (days)", hint: "0 = never expires" },
  { name: "maxLoginAttempts", label: "Max failed login attempts" },
  { name: "lockoutDurationMinutes", label: "Lockout duration (minutes)" },
  { name: "sessionTimeoutMinutes", label: "Session timeout (minutes)" },
];

export default function SecuritySettingsPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => securitySettingsApi.get(), []);

  const { register, control, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<SecuritySettingsInput>({ resolver: zodResolver(securitySettingsSchema) });

  useEffect(() => {
    if (data) reset(data);
  }, [data, reset]);

  async function onSubmit(values: SecuritySettingsInput) {
    try {
      await securitySettingsApi.update(values);
      toast.success("Security settings updated — takes effect immediately");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can edit security settings." />;
  }

  return (
    <div>
      <PageHeader title="Security Settings" description="Password policy, login lockout and session rules. Applied platform-wide." />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-96 w-full" />}

      {data && !error && (
        <Card>
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
              <div className="grid gap-5 sm:grid-cols-2">
                {numberFields.map((f) => (
                  <div key={f.name} className="space-y-1.5">
                    <Label htmlFor={f.name}>{f.label}</Label>
                    <Input id={f.name} type="number" {...register(f.name, { valueAsNumber: true })} />
                    {f.hint && <p className="text-xs text-muted-foreground">{f.hint}</p>}
                    {errors[f.name] && <p className="text-xs text-destructive">{errors[f.name]?.message as string}</p>}
                  </div>
                ))}
              </div>

              <div className="space-y-4 border-t pt-6">
                {boolFields.map((f) => (
                  <div key={f.name} className="flex items-center justify-between">
                    <div>
                      <Label htmlFor={f.name}>{f.label}</Label>
                      {f.hint && <p className="text-xs text-muted-foreground">{f.hint}</p>}
                    </div>
                    <Controller
                      name={f.name}
                      control={control}
                      render={({ field: { value, onChange } }) => (
                        <Switch id={f.name} checked={!!value} onCheckedChange={onChange} />
                      )}
                    />
                  </div>
                ))}
              </div>

              <div className="space-y-1.5 border-t pt-6">
                <Label htmlFor="allowedIpRanges">Allowed IP Ranges</Label>
                <Input id="allowedIpRanges" placeholder="e.g. 203.0.113.0/24, 198.51.100.10 (blank = unrestricted)" {...register("allowedIpRanges")} />
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
