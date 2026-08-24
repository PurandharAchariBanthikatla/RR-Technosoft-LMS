"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ShieldAlert, Loader2, Plus, Trash2, SlidersHorizontal } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { systemSettingsApi } from "@/lib/api/administration";
import { extractErrorMessage } from "@/lib/api/client";
import { Role } from "@/lib/constants";
import { systemSettingSchema, type SystemSettingInput } from "@/lib/validations/administration";
import { SystemSetting } from "@/types";

export default function SystemSettingsPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data: settings, isLoading, error, refetch } = useFetch(() => systemSettingsApi.list(), []);
  const [createOpen, setCreateOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<SystemSetting | null>(null);
  const [editingValues, setEditingValues] = useState<Record<string, string>>({});
  const [savingKey, setSavingKey] = useState<string | null>(null);

  const form = useForm<SystemSettingInput>({
    resolver: zodResolver(systemSettingSchema),
    defaultValues: { valueType: "STRING", category: "GENERAL" },
  });

  async function onCreate(values: SystemSettingInput) {
    try {
      await systemSettingsApi.create(values);
      toast.success("Setting created");
      setCreateOpen(false);
      form.reset({ valueType: "STRING", category: "GENERAL" });
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function saveValue(setting: SystemSetting) {
    const value = editingValues[setting.id] ?? setting.value ?? "";
    setSavingKey(setting.id);
    try {
      await systemSettingsApi.updateValue(setting.id, value);
      toast.success(`${setting.key} updated`);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSavingKey(null);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await systemSettingsApi.remove(deleteTarget.id);
      toast.success("Setting removed");
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return <EmptyState icon={ShieldAlert} title="Super Admins only" description="Only a Super Admin can manage system settings." />;
  }

  return (
    <div>
      <PageHeader
        title="System Settings"
        description="Platform-wide preferences. Grouped by category, editable inline."
        actions={
          <Button onClick={() => setCreateOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" /> New Setting
          </Button>
        }
      />

      {error && <ErrorState message={error} onRetry={refetch} />}
      {isLoading && !error && <Skeleton className="h-96 w-full" />}

      {settings && !error && (
        settings.length === 0 ? (
          <EmptyState icon={SlidersHorizontal} title="No settings yet" description="Add your first system setting." />
        ) : (
          <Card>
            <CardContent className="divide-y pt-6">
              {settings.map((s) => (
                <div key={s.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-medium">{s.key}</span>
                      <Badge variant="outline" className="text-[10px]">{s.category}</Badge>
                      <Badge variant="secondary" className="text-[10px]">{s.valueType}</Badge>
                    </div>
                    {s.description && <p className="text-xs text-muted-foreground">{s.description}</p>}
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <Input
                      className="w-56"
                      disabled={!s.isEditable}
                      defaultValue={s.value ?? ""}
                      onChange={(e) => setEditingValues((prev) => ({ ...prev, [s.id]: e.target.value }))}
                    />
                    <Button size="sm" variant="outline" disabled={!s.isEditable || savingKey === s.id} onClick={() => saveValue(s)}>
                      {savingKey === s.id ? <Loader2 className="h-4 w-4 animate-spin" /> : "Save"}
                    </Button>
                    {s.isEditable && (
                      <Button size="icon" variant="ghost" onClick={() => setDeleteTarget(s)}>
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        )
      )}

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New System Setting</DialogTitle>
            <DialogDescription>Add a platform-wide key/value preference.</DialogDescription>
          </DialogHeader>
          <form onSubmit={form.handleSubmit(onCreate)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="key">Key</Label>
              <Input id="key" placeholder="e.g. academics.passing_percentage" {...form.register("key")} />
              {form.formState.errors.key && <p className="text-xs text-destructive">{form.formState.errors.key.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="value">Value</Label>
              <Input id="value" {...form.register("value")} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Value Type</Label>
                <Select defaultValue="STRING" onValueChange={(v) => form.setValue("valueType", v as SystemSettingInput["valueType"])}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {["STRING", "NUMBER", "BOOLEAN", "JSON"].map((t) => (
                      <SelectItem key={t} value={t}>{t}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Category</Label>
                <Select defaultValue="GENERAL" onValueChange={(v) => form.setValue("category", v as SystemSettingInput["category"])}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {["GENERAL", "ACADEMICS", "ENGAGEMENT", "SECURITY", "INTEGRATIONS"].map((t) => (
                      <SelectItem key={t} value={t}>{t}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="description">Description</Label>
              <Input id="description" {...form.register("description")} />
            </div>
            <DialogFooter>
              <Button type="submit" disabled={form.formState.isSubmitting} className="gap-2">
                {form.formState.isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Create
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Remove setting?"
        description={`"${deleteTarget?.key}" will be permanently removed.`}
        confirmLabel="Remove"
        destructive
        onConfirm={handleDelete}
      />
    </div>
  );
}
