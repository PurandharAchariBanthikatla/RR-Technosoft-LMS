"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { useFetch } from "@/hooks/use-fetch";
import { paymentsApi } from "@/lib/api/finance";
import { extractErrorMessage } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/lib/utils";
import { Payment, PaymentStatus } from "@/types";

const STATUS_OPTIONS: PaymentStatus[] = [
  "INITIATED", "PENDING", "SUCCESS", "FAILED", "CANCELLED", "REFUNDED", "PARTIALLY_REFUNDED",
];

export default function AdminPaymentsPage() {
  const router = useRouter();
  const [status, setStatus] = useState<string>("all");
  const { data, isLoading, error, refetch } = useFetch(
    () => paymentsApi.list({ status: status === "all" ? undefined : (status as PaymentStatus), page: 0, size: 50 }),
    [status]
  );

  const [refundTarget, setRefundTarget] = useState<Payment | null>(null);
  const [refundAmount, setRefundAmount] = useState("");
  const [refundReason, setRefundReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function onRefund() {
    if (!refundTarget) return;
    if (!refundAmount || Number(refundAmount) <= 0 || !refundReason.trim()) {
      toast.error("Enter a refund amount and reason");
      return;
    }
    setSubmitting(true);
    try {
      await paymentsApi.refund(refundTarget.id, { amount: Number(refundAmount), reason: refundReason.trim() });
      toast.success("Refund requested");
      setRefundTarget(null);
      setRefundAmount("");
      setRefundReason("");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  const columns: Column<Payment>[] = [
    { key: "student", header: "Student", render: (p) => p.studentName },
    { key: "amount", header: "Amount", render: (p) => formatCurrency(p.amount, p.currency) },
    { key: "method", header: "Method", render: (p) => p.method ?? "—" },
    { key: "gateway", header: "Gateway", render: (p) => p.gatewayProvider ?? "—" },
    { key: "status", header: "Status", render: (p) => <StatusBadge status={p.status} /> },
    { key: "refunded", header: "Refunded", render: (p) => (p.refundedAmount > 0 ? formatCurrency(p.refundedAmount, p.currency) : "—") },
    { key: "paidAt", header: "Paid at", render: (p) => (p.paidAt ? formatDate(p.paidAt) : "—") },
    {
      key: "actions",
      header: "",
      className: "text-right",
      render: (p) => (
        <div className="flex justify-end gap-2">
          {p.status === "SUCCESS" && p.refundedAmount < p.amount && (
            <Button
              variant="outline" size="sm"
              onClick={(e) => {
                e.stopPropagation();
                setRefundTarget(p);
                setRefundAmount(String(p.amount - p.refundedAmount));
              }}
            >
              Refund
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="Payments" description="Every payment attempt across the gateway and manual entries, with refund controls." />

      <div className="mb-4">
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-full sm:w-56"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUS_OPTIONS.map((s) => (
              <SelectItem key={s} value={s}>{s.replace("_", " ")}</SelectItem>
            ))}
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
          rowKey={(p) => p.id}
          emptyTitle="No payments found"
          onRowClick={(p) => router.push(`/admin/finance/student-fees/${p.studentFeeId}`)}
        />
      )}

      <Dialog open={!!refundTarget} onOpenChange={(open) => !open && setRefundTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Refund this payment</DialogTitle>
            <DialogDescription>
              {refundTarget && `${refundTarget.studentName} paid ${formatCurrency(refundTarget.amount, refundTarget.currency)}.`}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="refundAmount">Amount {refundTarget && `(${refundTarget.currency})`}</Label>
              <Input id="refundAmount" type="number" min={0} step="0.01" value={refundAmount} onChange={(e) => setRefundAmount(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="refundReason">Reason</Label>
              <Textarea id="refundReason" rows={2} value={refundReason} onChange={(e) => setRefundReason(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRefundTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={onRefund} disabled={submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />} Request refund
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
