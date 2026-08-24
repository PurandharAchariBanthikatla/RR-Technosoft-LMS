"use client";

import { useState } from "react";
import { Plus, MoreHorizontal, Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { feeStructuresApi, type FeeInstallmentInput } from "@/lib/api/finance";
import { coursesApi } from "@/lib/api/courses";
import { extractErrorMessage } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/lib/utils";
import { FeeStructure } from "@/types";

function emptyInstallment(n: number): FeeInstallmentInput {
  return { installmentNumber: n, amount: 0, dueAfterDays: 0 };
}

export default function AdminFeeStructuresPage() {
  const [activeOnly, setActiveOnly] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<FeeStructure | null>(null);
  const [deactivating, setDeactivating] = useState(false);

  const { data, isLoading, error, refetch } = useFetch(
    () => feeStructuresApi.list({ activeOnly, page: 0, size: 50 }),
    [activeOnly]
  );
  const { data: courses } = useFetch(() => coursesApi.list({ page: 0, size: 200 }), []);

  // --- create form state ---
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [courseId, setCourseId] = useState<string>("none");
  const [totalAmount, setTotalAmount] = useState("");
  const [currency, setCurrency] = useState("INR");
  const [installments, setInstallments] = useState<FeeInstallmentInput[]>([emptyInstallment(1)]);
  const [submitting, setSubmitting] = useState(false);

  function resetForm() {
    setName("");
    setDescription("");
    setCourseId("none");
    setTotalAmount("");
    setCurrency("INR");
    setInstallments([emptyInstallment(1)]);
  }

  function addInstallment() {
    setInstallments((prev) => [...prev, emptyInstallment(prev.length + 1)]);
  }

  function removeInstallment(idx: number) {
    setInstallments((prev) =>
      prev.filter((_, i) => i !== idx).map((row, i) => ({ ...row, installmentNumber: i + 1 }))
    );
  }

  function updateInstallment(idx: number, patch: Partial<FeeInstallmentInput>) {
    setInstallments((prev) => prev.map((row, i) => (i === idx ? { ...row, ...patch } : row)));
  }

  const installmentTotal = installments.reduce((sum, i) => sum + (Number(i.amount) || 0), 0);

  async function onCreate() {
    if (!name.trim()) {
      toast.error("Name is required");
      return;
    }
    if (!totalAmount || Number(totalAmount) <= 0) {
      toast.error("Total amount must be greater than zero");
      return;
    }
    if (installments.length === 0) {
      toast.error("Add at least one installment");
      return;
    }
    if (Math.round(installmentTotal) !== Math.round(Number(totalAmount))) {
      toast.error(`Installments must add up to the total amount (currently ${formatCurrency(installmentTotal, currency)})`);
      return;
    }
    setSubmitting(true);
    try {
      await feeStructuresApi.create({
        name: name.trim(),
        description: description.trim() || undefined,
        courseId: courseId === "none" ? undefined : courseId,
        totalAmount: Number(totalAmount),
        currency,
        installments: installments.map((i) => ({
          installmentNumber: i.installmentNumber,
          amount: Number(i.amount),
          dueAfterDays: Number(i.dueAfterDays),
        })),
      });
      toast.success("Fee structure created");
      setCreateOpen(false);
      resetForm();
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onDeactivate() {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await feeStructuresApi.deactivate(deactivateTarget.id);
      toast.success(`"${deactivateTarget.name}" deactivated`);
      setDeactivateTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDeactivating(false);
    }
  }

  const columns: Column<FeeStructure>[] = [
    {
      key: "name",
      header: "Fee structure",
      render: (f) => (
        <div>
          <p className="font-medium">{f.name}</p>
          <p className="text-xs text-muted-foreground">{f.courseTitle ?? "All courses"}</p>
        </div>
      ),
    },
    { key: "amount", header: "Total amount", render: (f) => formatCurrency(f.totalAmount, f.currency) },
    { key: "installments", header: "Installments", render: (f) => f.installmentCount },
    {
      key: "status",
      header: "Status",
      render: (f) => <Badge variant={f.isActive ? "success" : "outline"}>{f.isActive ? "Active" : "Inactive"}</Badge>,
    },
    { key: "created", header: "Created", render: (f) => formatDate(f.createdAt) },
    {
      key: "actions",
      header: "",
      className: "text-right",
      render: (f) => (
        <div className="flex justify-end">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon"><MoreHorizontal className="h-4 w-4" /></Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem disabled className="text-xs text-muted-foreground">
                {f.installments.map((i) => `#${i.installmentNumber}: ${formatCurrency(i.amount, f.currency)} (+${i.dueAfterDays}d)`).join(" · ")}
              </DropdownMenuItem>
              {f.isActive && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => setDeactivateTarget(f)}>
                    Deactivate
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Fee Structures"
        description="Reusable fee templates that can be assigned to students, optionally scoped to a course."
        actions={<Button className="gap-2" onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4" /> New fee structure</Button>}
      />

      <div className="mb-4">
        <Select value={activeOnly ? "active" : "all"} onValueChange={(v) => setActiveOnly(v === "active")}>
          <SelectTrigger className="w-full sm:w-48"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="active">Active only</SelectItem>
            <SelectItem value="all">All (incl. inactive)</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.content ?? []}
          isLoading={isLoading}
          rowKey={(f) => f.id}
          emptyTitle="No fee structures yet"
          emptyDescription="Create one to start assigning fees to students."
        />
      )}

      <Dialog open={createOpen} onOpenChange={(open) => { setCreateOpen(open); if (!open) resetForm(); }}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>New fee structure</DialogTitle>
            <DialogDescription>
              Define a total amount and how it splits into installments. Installment amounts must add up to the total.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="fsName">Name</Label>
              <Input id="fsName" placeholder="Full Stack Development — Standard" value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fsDescription">Description</Label>
              <Textarea id="fsDescription" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Course (optional)</Label>
                <Select value={courseId} onValueChange={setCourseId}>
                  <SelectTrigger><SelectValue placeholder="All courses" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">All courses</SelectItem>
                    {courses?.content.map((c) => (
                      <SelectItem key={c.id} value={c.id}>{c.title}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="fsCurrency">Currency</Label>
                <Input id="fsCurrency" value={currency} onChange={(e) => setCurrency(e.target.value.toUpperCase())} maxLength={3} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fsTotal">Total amount</Label>
              <Input id="fsTotal" type="number" min={0} step="0.01" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} />
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label>Installments</Label>
                <Button type="button" variant="outline" size="sm" onClick={addInstallment}>Add row</Button>
              </div>
              <div className="space-y-2">
                {installments.map((row, idx) => (
                  <div key={idx} className="flex items-center gap-2">
                    <span className="w-6 shrink-0 text-xs text-muted-foreground">#{row.installmentNumber}</span>
                    <Input
                      type="number" min={0} step="0.01" placeholder="Amount"
                      value={row.amount || ""}
                      onChange={(e) => updateInstallment(idx, { amount: Number(e.target.value) })}
                    />
                    <Input
                      type="number" min={0} placeholder="Due after (days)"
                      value={row.dueAfterDays ?? ""}
                      onChange={(e) => updateInstallment(idx, { dueAfterDays: Number(e.target.value) })}
                    />
                    <Button
                      type="button" variant="ghost" size="icon"
                      disabled={installments.length === 1}
                      onClick={() => removeInstallment(idx)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">
                Installments total: {formatCurrency(installmentTotal, currency)}
                {totalAmount && Math.round(installmentTotal) !== Math.round(Number(totalAmount)) && (
                  <span className="text-destructive"> — must equal {formatCurrency(Number(totalAmount), currency)}</span>
                )}
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="button" onClick={onCreate} disabled={submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              Create fee structure
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deactivateTarget}
        onOpenChange={(open) => !open && setDeactivateTarget(null)}
        title="Deactivate this fee structure?"
        description={`"${deactivateTarget?.name}" will no longer be assignable to new students. Students already assigned are unaffected.`}
        confirmLabel="Deactivate"
        destructive
        loading={deactivating}
        onConfirm={onDeactivate}
      />
    </div>
  );
}
