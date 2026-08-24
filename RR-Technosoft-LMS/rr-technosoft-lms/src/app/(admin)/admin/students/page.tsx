"use client";

import { useState } from "react";
import { Search, Plus, MoreHorizontal, Loader2, Pencil } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { useFetch } from "@/hooks/use-fetch";
import { useDebounce } from "@/hooks/use-debounce";
import { studentsApi } from "@/lib/api/students";
import { extractErrorMessage } from "@/lib/api/client";
import {
  createStudentSchema, type CreateStudentInput,
  updateStudentSchema, type UpdateStudentInput,
} from "@/lib/validations/auth";
import { AccountStatus } from "@/lib/constants";
import { initials, formatDate } from "@/lib/utils";
import { User } from "@/types";

export default function AdminStudentsPage() {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search);
  const [addOpen, setAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<User | null>(null);
  const [suspendTarget, setSuspendTarget] = useState<User | null>(null);
  const [statusSubmitting, setStatusSubmitting] = useState(false);

  const { data, isLoading, error, refetch } = useFetch(
    () => studentsApi.list({ search: debouncedSearch || undefined, page: 0, size: 20 }),
    [debouncedSearch]
  );

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateStudentInput>({ resolver: zodResolver(createStudentSchema) });

  const {
    register: registerEdit,
    handleSubmit: handleEditSubmit,
    reset: resetEdit,
    formState: { errors: editErrors, isSubmitting: isEditSubmitting },
  } = useForm<UpdateStudentInput>({ resolver: zodResolver(updateStudentSchema) });

  async function onCreate(values: CreateStudentInput) {
    try {
      const created = await studentsApi.create(values);
      toast.success(`${created.name} added — Student ID: ${created.studentId}`);
      setAddOpen(false);
      reset();
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  function openEdit(user: User) {
    // batch/branch/college/graduationYear aren't on the list-row response
    // (see UserSummaryResponse) — leaving those blank here is safe, the
    // backend only overwrites a profile field when it's actually submitted.
    resetEdit({ fullName: user.name, phone: user.phone ?? "" });
    setEditTarget(user);
  }

  async function onEdit(values: UpdateStudentInput) {
    if (!editTarget) return;
    try {
      const updated = await studentsApi.update(editTarget.id, values);
      toast.success(`${updated.name} updated`);
      setEditTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  async function handleToggleStatus(user: User, status: AccountStatus) {
    setStatusSubmitting(true);
    try {
      await studentsApi.setStatus(user.id, status);
      toast.success(`${user.name} is now ${status.toLowerCase()}`);
      setSuspendTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setStatusSubmitting(false);
    }
  }

  const columns: Column<User>[] = [
    {
      key: "name",
      header: "Student",
      render: (u) => (
        <div className="flex items-center gap-3">
          <Avatar className="h-8 w-8">
            <AvatarImage src={u.avatarUrl} />
            <AvatarFallback>{initials(u.name)}</AvatarFallback>
          </Avatar>
          <div>
            <p className="font-medium">{u.name}</p>
            <p className="text-xs text-muted-foreground font-mono">{u.studentId ?? "—"}</p>
          </div>
        </div>
      ),
    },
    { key: "phone", header: "Phone", render: (u) => u.phone ?? "—" },
    { key: "status", header: "Status", render: (u) => <StatusBadge status={u.status ?? "ACTIVE"} /> },
    { key: "joined", header: "Joined", render: (u) => formatDate(u.createdAt) },
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
              <DropdownMenuItem onClick={() => openEdit(u)}>
                <Pencil className="mr-2 h-3.5 w-3.5" /> Edit details
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => handleToggleStatus(u, AccountStatus.ACTIVE)}>
                Reactivate
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => setSuspendTarget(u)}>
                Suspend account
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
        title="Students"
        description="All students enrolled across RR TECHNOSOFT programs."
        actions={<Button className="gap-2" onClick={() => setAddOpen(true)}><Plus className="h-4 w-4" /> Add student</Button>}
      />

      <div className="relative mb-4 max-w-sm">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input placeholder="Search by name or Student ID..." className="pl-8" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(u) => u.id} emptyTitle="No students found" />
      )}

      <Dialog open={addOpen} onOpenChange={setAddOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add a student</DialogTitle>
            <DialogDescription>
              A Student ID (e.g. RRT2026S0001) is generated automatically — share the initial
              password with them so they can sign in and change it.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSubmit(onCreate)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="fullName">Full name</Label>
              <Input id="fullName" {...register("fullName")} />
              {errors.fullName && <p className="text-xs text-destructive">{errors.fullName.message}</p>}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="phone">Phone</Label>
                <Input id="phone" {...register("phone")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="initialPassword">Initial password</Label>
                <Input id="initialPassword" type="text" {...register("initialPassword")} />
                {errors.initialPassword && <p className="text-xs text-destructive">{errors.initialPassword.message}</p>}
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="batch">Batch</Label>
                <Input id="batch" placeholder="2026-A" {...register("batch")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="branch">Branch</Label>
                <Input id="branch" placeholder="CSE" {...register("branch")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="graduationYear">Grad. year</Label>
                <Input id="graduationYear" type="number" {...register("graduationYear")} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="college">College</Label>
              <Input id="college" {...register("college")} />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setAddOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Create student
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!editTarget} onOpenChange={(open) => !open && setEditTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit student</DialogTitle>
            <DialogDescription>
              Student ID, password, and account status are managed separately and aren&apos;t changed here.
              Academic fields (batch/branch/college/grad. year) left blank keep their current value.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleEditSubmit(onEdit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="edit-fullName">Full name</Label>
              <Input id="edit-fullName" {...registerEdit("fullName")} />
              {editErrors.fullName && <p className="text-xs text-destructive">{editErrors.fullName.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="edit-phone">Phone</Label>
              <Input id="edit-phone" {...registerEdit("phone")} />
              {editErrors.phone && <p className="text-xs text-destructive">{editErrors.phone.message}</p>}
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="edit-batch">Batch</Label>
                <Input id="edit-batch" placeholder="2026-A" {...registerEdit("batch")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="edit-branch">Branch</Label>
                <Input id="edit-branch" placeholder="CSE" {...registerEdit("branch")} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="edit-graduationYear">Grad. year</Label>
                <Input id="edit-graduationYear" type="number" {...registerEdit("graduationYear")} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="edit-college">College</Label>
              <Input id="edit-college" {...registerEdit("college")} />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setEditTarget(null)}>Cancel</Button>
              <Button type="submit" disabled={isEditSubmitting}>
                {isEditSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                Save changes
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!suspendTarget}
        onOpenChange={(open) => !open && setSuspendTarget(null)}
        title="Suspend this student?"
        description={`${suspendTarget?.name} will no longer be able to sign in until reactivated.`}
        confirmLabel="Suspend"
        destructive
        loading={statusSubmitting}
        onConfirm={() => suspendTarget && handleToggleStatus(suspendTarget, AccountStatus.SUSPENDED)}
      />
    </div>
  );
}
