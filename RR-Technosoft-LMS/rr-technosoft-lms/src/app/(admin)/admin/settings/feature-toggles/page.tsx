"use client";

import { toast } from "sonner";
import { ShieldAlert, ToggleLeft } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { Card, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { featureTogglesApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";

export default function FeatureTogglesPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data: toggles, isLoading, error, refetch } = useFetch(() => featureTogglesApi.list(), []);

  async function handleToggle(featureKey: string, enabled: boolean) {
    try {
      await featureTogglesApi.update(featureKey, enabled);
      toast.success(`${featureKey} ${enabled ? "enabled" : "disabled"}`);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can manage feature toggles." />;
  }

  return (
    <div>
      <PageHeader title="Feature Toggles" description="Turn platform features on or off for everyone, instantly." />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-64 w-full" />}

      {toggles && !error && (
        toggles.length === 0 ? (
          <EmptyState icon={ToggleLeft} title="No feature toggles configured" />
        ) : (
          <Card>
            <CardContent className="divide-y pt-6">
              {toggles.map((t) => (
                <div key={t.id} className="flex items-center justify-between gap-4 py-4">
                  <div>
                    <div className="font-medium">{t.name}</div>
                    {t.description && <p className="text-sm text-muted-foreground">{t.description}</p>}
                  </div>
                  <Switch checked={t.enabled} onCheckedChange={(v) => handleToggle(t.featureKey, v)} />
                </div>
              ))}
            </CardContent>
          </Card>
        )
      )}
    </div>
  );
}
