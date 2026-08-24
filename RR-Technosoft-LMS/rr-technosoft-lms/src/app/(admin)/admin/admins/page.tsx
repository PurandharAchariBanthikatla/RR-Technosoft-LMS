"use client";

import { useState } from "react";
import { Plus, MoreHorizontal, Loader2, ShieldAlert } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { adminsApi } from "@/lib/api/admins";
import { extractErrorMessage } from "@/lib/api/client";
import { createAdminSchema, type CreateAdminInput } from "@/lib/validations/auth";
import { AccountStatus, Role } from "@/lib/constants";
import { formatDate } from "@/lib/utils";
import { User } from "@/types";
import { EmptyState } from "@/components/shared/empty-state";

export default function AdminsPage() {
  const currentUser = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => adminsApi.list({ page: 0, size: 20 }), []);
  const [addOpen, setAddOpen] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<User | null>(null);
  const [removing, setRemoving] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateAdminInput>({ resolver: zodResolver(createAdminSchema) });

  // Defense in depth: this page is also blocked at the URL layer on the backend
  // (/admins/** -> hasRole('SUPER_ADMIN')), so a non-super-admin's requests
  // would 403 even if this check were bypassed client-side.
  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return (
      <EmptyState
        icon={ShieldAlert}
        title="Super Admins only"
        description="Only a Super Admin can view or manage other admin accounts."
      />
    );
  }

  async function onCreate(values: CreateAdminInput) {
    try {
      const created = await adminsApi.create(values);
      toast.success(`${created.name} added as an admin`);
      setAddOpen(false);
      reset();
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function handleRemove() {
    if (!removeTarget) return;
    setRemoving(true);
    try {
      await adminsApi.remove(removeTarget.id);
      toast.success(`${removeTarget.name} was removed`);
      setRemoveTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setRemoving(false);
    }
  }

  const columns: Column<User>[] = [
    {
      key: "name",
      header: "Admin",
      render: (u) => (
        <div>
          <p className="font-medium">{u.name}</p>
          <p className="text-xs text-muted-foreground">{u.email}</p>
        </div>
      ),
    },
    { key: "phone", header: "Phone", render: (u) => u.phone ?? "—" },
    { key: "status", header: "Status", render: (u) => <StatusBadge status={u.status ?? "ACTIVE"} /> },
    { key: "joined", header: "Added on", render: (u) => formatDate(u.createdAt) },
    {
      key: "actions",
      header: "",
      className: "text-right",
      render: (u) => (
        <div className="flex justify-end">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon"><MoreHorizontal className="h-4 w-4" /></Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => adminsApi.setStatus(u.id, AccountStatus.SUSPENDED).then(refetch)}>
                Suspend
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => setRemoveTarget(u)}>
                Remove admin
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Admins"
        description="Manage who has admin access to RR TECHNOSOFT LMS. Super Admin only."
        actions={<Button className="gap-2" onClick={() => setAddOpen(true)}><Plus className="h-4 w-4" /> Add admin</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(u) => u.id} emptyTitle="No admins found" />
      )}

      <Dialog open={addOpen} onOpenChange={setAddOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add an admin</DialogTitle>
            <DialogDescription>They&apos;ll sign in with this email and password.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit(onCreate)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="fullName">Full name</Label>
              <Input id="fullName" {...register("fullName")} />
              {errors.fullName && <p className="text-xs text-destructive">{errors.fullName.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="email">Email address</Label>
              <Input id="email" type="email" {...register("email")} />
              {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="text" {...register("password")} />
              {errors.password && <p className="text-xs text-destructive">{errors.password.message}</p>}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="phone">Phone</Label>
                <Input id="phone" {...register("phone")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="designation">Designation</Label>
                <Input id="designation" placeholder="Program Manager" {...register("designation")} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="department">Department</Label>
              <Input id="department" {...register("department")} />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setAddOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Create admin
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!removeTarget}
        onOpenChange={(open) => !open && setRemoveTarget(null)}
        title="Remove this admin?"
        description={`${removeTarget?.name} will permanently lose admin access.`}
        confirmLabel="Remove admin"
        destructive
        loading={removing}
        onConfirm={handleRemove}
      />
    </div>
  );
}
