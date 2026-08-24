"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
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
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { organizationProfileApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { organizationProfileSchema, type OrganizationProfileInput } from "@/lib/validations/administration";

export default function OrganizationProfilePage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => organizationProfileApi.get(), []);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<OrganizationProfileInput>({ resolver: zodResolver(organizationProfileSchema) });

  useEffect(() => {
    if (data) reset(data);
  }, [data, reset]);

  async function onSubmit(values: OrganizationProfileInput) {
    try {
      await organizationProfileApi.update(values);
      toast.success("Organization profile updated");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return (
      <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can edit the organization profile." />
    );
  }

  const field = (name: keyof OrganizationProfileInput, label: string, opts?: { placeholder?: string }) => (
    <div className="space-y-1.5">
      <Label htmlFor={name}>{label}</Label>
      <Input id={name} placeholder={opts?.placeholder} {...register(name)} />
      {errors[name] && <p className="text-xs text-destructive">{errors[name]?.message as string}</p>}
    </div>
  );

  return (
    <div>
      <PageHeader title="Organization Profile" description="Branding and contact details shown across the platform." />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-96 w-full" />}

      {data && !error && (
        <Card>
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit(onSubmit)} className="grid gap-5 sm:grid-cols-2">
              {field("orgName", "Organization Name")}
              {field("legalName", "Legal Name")}
              {field("logoUrl", "Logo URL", { placeholder: "https://..." })}
              {field("faviconUrl", "Favicon URL", { placeholder: "https://..." })}
              {field("website", "Website", { placeholder: "https://..." })}
              {field("supportEmail", "Support Email")}
              {field("supportPhone", "Support Phone")}
              {field("taxId", "Tax ID / GSTIN")}
              {field("addressLine1", "Address Line 1")}
              {field("addressLine2", "Address Line 2")}
              {field("city", "City")}
              {field("state", "State")}
              {field("country", "Country")}
              {field("postalCode", "Postal Code")}
              {field("timezone", "Timezone", { placeholder: "Asia/Kolkata" })}
              {field("dateFormat", "Date Format", { placeholder: "dd-MM-yyyy" })}

              <div className="sm:col-span-2 flex justify-end">
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
