"use client";

import { useMemo, useState } from "react";
import { toast } from "sonner";
import { ShieldAlert } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { Card, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { permissionsApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { PermissionMatrixEntry } from "@/types";

const EDITABLE_ROLES: Role[] = [Role.ADMIN, Role.STUDENT];

export default function PermissionMatrixPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => permissionsApi.getMatrix(), []);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  // Optimistic local overrides keyed by `${permissionId}:${role}`, so a toggle
  // flips instantly without waiting on the round trip.
  const [overrides, setOverrides] = useState<Record<string, boolean>>({});

  const grouped = useMemo(() => {
    if (!data) return [];
    const byCategory = new Map<string, typeof data.permissions>();
    for (const p of data.permissions) {
      const list = byCategory.get(p.category) ?? [];
      list.push(p);
      byCategory.set(p.category, list);
    }
    return Array.from(byCategory.entries());
  }, [data]);

  function entryFor(entries: PermissionMatrixEntry[], permissionId: string, role: Role) {
    const key = `${permissionId}:${role}`;
    if (key in overrides) return overrides[key];
    return entries.find((e) => e.permissionId === permissionId && e.role === role)?.allowed ?? false;
  }

  async function toggle(permissionId: string, role: Role, nextValue: boolean) {
    const key = `${permissionId}:${role}`;
    setOverrides((prev) => ({ ...prev, [key]: nextValue }));
    setSavingKey(key);
    try {
      await permissionsApi.updateMatrix([{ permissionId, role, allowed: nextValue }]);
    } catch (err) {
      // Roll back on failure.
      setOverrides((prev) => ({ ...prev, [key]: !nextValue }));
      toast.error(extractErrorMessage(err));
    } finally {
      setSavingKey(null);
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return (
      <EmptyState
        icon={ShieldAlert}
        title="Super Admins only"
        description="Only a Super Admin can view or edit the permission matrix."
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Permission Matrix"
        description="Grant or revoke what each role can do. Changes apply immediately, everywhere — no deployment required."
      />

      {error && <ErrorState message={error} onRetry={refetch} />}

      {isLoading && !error && (
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      )}

      {data && !error && (
        <div className="space-y-6">
          <Card className="border-dashed bg-muted/30">
            <CardContent className="flex flex-wrap items-center gap-x-6 gap-y-2 py-4 text-sm text-muted-foreground">
              <span>
                <strong className="text-foreground">SUPER_ADMIN</strong> always holds every permission and can&apos;t be
                edited here.
              </span>
            </CardContent>
          </Card>

          {grouped.map(([category, permissions]) => (
            <Card key={category}>
              <CardContent className="pt-6">
                <h3 className="mb-4 font-display text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                  {category}
                </h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b text-left text-xs uppercase text-muted-foreground">
                        <th className="py-2 pr-4 font-medium">Permission</th>
                        {EDITABLE_ROLES.map((role) => (
                          <th key={role} className="py-2 px-4 text-center font-medium">
                            {role}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {permissions.map((p) => (
                        <tr key={p.id} className="border-b last:border-0">
                          <td className="py-3 pr-4">
                            <div className="font-medium">{p.name}</div>
                            {p.description && (
                              <div className="text-xs text-muted-foreground">{p.description}</div>
                            )}
                          </td>
                          {EDITABLE_ROLES.map((role) => {
                            const key = `${p.id}:${role}`;
                            const checked = entryFor(data.entries, p.id, role);
                            return (
                              <td key={role} className="py-3 px-4 text-center">
                                <Switch
                                  checked={checked}
                                  disabled={savingKey === key}
                                  onCheckedChange={(v) => toggle(p.id, role, v)}
                                  aria-label={`${p.name} for ${role}`}
                                />
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
