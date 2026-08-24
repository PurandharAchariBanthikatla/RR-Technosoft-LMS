"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Loader2, FileDown, Plus } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { useFetch } from "@/hooks/use-fetch";
import { studentFeesApi, paymentsApi, openPdfInNewTab } from "@/lib/api/finance";
import { extractErrorMessage } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/lib/utils";
import { DiscountType, PaymentMethod } from "@/types";

const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "BANK_TRANSFER", "CHEQUE", "CARD", "UPI", "NETBANKING", "WALLET"];

export default function AdminStudentFeeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();

  const { data: fee, isLoading, error, refetch } = useFetch(() => studentFeesApi.get(id), [id]);
  const { data: payments, refetch: refetchPayments } = useFetch(
    () => paymentsApi.list({ studentFeeId: id, page: 0, size: 50 }),
    [id]
  );

  const [discountOpen, setDiscountOpen] = useState(false);
  const [discountType, setDiscountType] = useState<DiscountType>("FLAT");
  const [discountValue, setDiscountValue] = useState("");
  const [discountReason, setDiscountReason] = useState("");

  const [fineOpen, setFineOpen] = useState(false);
  const [fineAmount, setFineAmount] = useState("");
  const [fineReason, setFineReason] = useState("");
  const [fineInstallmentId, setFineInstallmentId] = useState<string>("none");

  const [manualOpen, setManualOpen] = useState(false);
  const [manualInstallmentId, setManualInstallmentId] = useState<string>("none");
  const [manualAmount, setManualAmount] = useState("");
  const [manualMethod, setManualMethod] = useState<PaymentMethod>("CASH");
  const [manualNote, setManualNote] = useState("");

  const [refundTargetId, setRefundTargetId] = useState<string | null>(null);
  const [refundAmount, setRefundAmount] = useState("");
  const [refundReason, setRefundReason] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [downloadingInvoice, setDownloadingInvoice] = useState(false);

  async function refetchAll() {
    refetch();
    refetchPayments();
  }

  async function onAddDiscount() {
    if (!discountValue || Number(discountValue) <= 0 || !discountReason.trim()) {
      toast.error("Enter a value and reason");
      return;
    }
    setSubmitting(true);
    try {
      await studentFeesApi.addDiscount(id, { type: discountType, value: Number(discountValue), reason: discountReason.trim() });
      toast.success("Discount applied");
      setDiscountOpen(false);
      setDiscountValue("");
      setDiscountReason("");
      refetchAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onAddFine() {
    if (!fineAmount || Number(fineAmount) <= 0 || !fineReason.trim()) {
      toast.error("Enter an amount and reason");
      return;
    }
    setSubmitting(true);
    try {
      await studentFeesApi.addFine(id, {
        amount: Number(fineAmount),
        reason: fineReason.trim(),
        installmentId: fineInstallmentId === "none" ? undefined : fineInstallmentId,
      });
      toast.success("Fine added");
      setFineOpen(false);
      setFineAmount("");
      setFineReason("");
      setFineInstallmentId("none");
      refetchAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onRecordManual() {
    if (!manualAmount || Number(manualAmount) <= 0) {
      toast.error("Enter a valid amount");
      return;
    }
    setSubmitting(true);
    try {
      await paymentsApi.recordManual({
        studentFeeId: id,
        installmentId: manualInstallmentId === "none" ? undefined : manualInstallmentId,
        amount: Number(manualAmount),
        method: manualMethod,
        note: manualNote.trim() || undefined,
      });
      toast.success("Payment recorded and receipt generated");
      setManualOpen(false);
      setManualAmount("");
      setManualNote("");
      setManualInstallmentId("none");
      refetchAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onRefund() {
    if (!refundTargetId || !refundAmount || Number(refundAmount) <= 0 || !refundReason.trim()) {
      toast.error("Enter a refund amount and reason");
      return;
    }
    setSubmitting(true);
    try {
      await paymentsApi.refund(refundTargetId, { amount: Number(refundAmount), reason: refundReason.trim() });
      toast.success("Refund requested");
      setRefundTargetId(null);
      setRefundAmount("");
      setRefundReason("");
      refetchAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onDownloadInvoice() {
    setDownloadingInvoice(true);
    try {
      await openPdfInNewTab(studentFeesApi.invoicePdfUrl(id), `invoice-${id}.pdf`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDownloadingInvoice(false);
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error || !fee) {
    return <ErrorState message={error ?? "Fee record not found"} onRetry={refetch} />;
  }

  return (
    <div>
      <Button variant="ghost" size="sm" className="mb-2 gap-1.5" onClick={() => router.push("/admin/finance/student-fees")}>
        <ArrowLeft className="h-4 w-4" /> Back to student fees
      </Button>
      <PageHeader
        title={fee.studentName}
        description={`${fee.courseTitle ?? "No course"} · ${fee.feeStructureName ?? "Custom fee"}`}
        actions={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" className="gap-1.5" disabled={downloadingInvoice} onClick={onDownloadInvoice}>
              {downloadingInvoice ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileDown className="h-4 w-4" />}
              Invoice
            </Button>
            <Button variant="outline" size="sm" onClick={() => setDiscountOpen(true)}>Add discount</Button>
            <Button variant="outline" size="sm" onClick={() => setFineOpen(true)}>Add fine</Button>
            <Button size="sm" className="gap-1.5" onClick={() => setManualOpen(true)}><Plus className="h-4 w-4" /> Record payment</Button>
          </div>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <SummaryCard label="Total amount" value={formatCurrency(fee.totalAmount, fee.currency)} />
        <SummaryCard label="Discount" value={`- ${formatCurrency(fee.discountAmount, fee.currency)}`} />
        <SummaryCard label="Fine" value={`+ ${formatCurrency(fee.fineAmount, fee.currency)}`} />
        <SummaryCard label="Paid" value={formatCurrency(fee.amountPaid, fee.currency)} />
        <SummaryCard label="Balance due" value={formatCurrency(fee.balanceDue, fee.currency)} highlight />
      </div>

      <div className="mb-2 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold">Installments</h2>
        <StatusBadge status={fee.status} />
      </div>
      <Card className="mb-6">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>#</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Due date</TableHead>
                <TableHead>Paid</TableHead>
                <TableHead>Balance</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Paid at</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {fee.installments.map((i) => (
                <TableRow key={i.id}>
                  <TableCell>{i.installmentNumber}</TableCell>
                  <TableCell>{formatCurrency(i.amount, fee.currency)}</TableCell>
                  <TableCell>{formatDate(i.dueDate)}</TableCell>
                  <TableCell>{formatCurrency(i.paidAmount, fee.currency)}</TableCell>
                  <TableCell>{formatCurrency(i.balanceDue, fee.currency)}</TableCell>
                  <TableCell><StatusBadge status={i.status} /></TableCell>
                  <TableCell>{i.paidAt ? formatDate(i.paidAt) : "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <h2 className="mb-2 font-display text-lg font-semibold">Payments</h2>
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Amount</TableHead>
                <TableHead>Method</TableHead>
                <TableHead>Gateway</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Refunded</TableHead>
                <TableHead>Paid at</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(payments?.content ?? []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{formatCurrency(p.amount, p.currency)}</TableCell>
                  <TableCell>{p.method ?? "—"}</TableCell>
                  <TableCell>{p.gatewayProvider ?? "—"}</TableCell>
                  <TableCell><StatusBadge status={p.status} /></TableCell>
                  <TableCell>{p.refundedAmount > 0 ? formatCurrency(p.refundedAmount, p.currency) : "—"}</TableCell>
                  <TableCell>{p.paidAt ? formatDate(p.paidAt) : "—"}</TableCell>
                  <TableCell className="text-right">
                    {p.status === "SUCCESS" && p.refundedAmount < p.amount && (
                      <Button
                        variant="ghost" size="sm"
                        onClick={() => { setRefundTargetId(p.id); setRefundAmount(String(p.amount - p.refundedAmount)); }}
                      >
                        Refund
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {(payments?.content?.length ?? 0) === 0 && (
                <TableRow><TableCell colSpan={7} className="text-center text-sm text-muted-foreground py-8">No payments recorded yet.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Add discount */}
      <Dialog open={discountOpen} onOpenChange={setDiscountOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add a discount</DialogTitle>
            <DialogDescription>Reduces the net payable amount for this student&apos;s fee.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Type</Label>
              <Select value={discountType} onValueChange={(v) => setDiscountType(v as DiscountType)}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="FLAT">Flat amount</SelectItem>
                  <SelectItem value="PERCENTAGE">Percentage</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="discountValue">{discountType === "PERCENTAGE" ? "Percentage (%)" : `Amount (${fee.currency})`}</Label>
              <Input id="discountValue" type="number" min={0} step="0.01" value={discountValue} onChange={(e) => setDiscountValue(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="discountReason">Reason</Label>
              <Textarea id="discountReason" rows={2} value={discountReason} onChange={(e) => setDiscountReason(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDiscountOpen(false)}>Cancel</Button>
            <Button onClick={onAddDiscount} disabled={submitting}>{submitting && <Loader2 className="h-4 w-4 animate-spin" />} Apply discount</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add fine */}
      <Dialog open={fineOpen} onOpenChange={setFineOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add a fine</DialogTitle>
            <DialogDescription>Increases the net payable amount, e.g. for a late payment.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Installment (optional)</Label>
              <Select value={fineInstallmentId} onValueChange={setFineInstallmentId}>
                <SelectTrigger><SelectValue placeholder="Not tied to a specific installment" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Not tied to a specific installment</SelectItem>
                  {fee.installments.map((i) => (
                    <SelectItem key={i.id} value={i.id}>Installment #{i.installmentNumber}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fineAmount">Amount ({fee.currency})</Label>
              <Input id="fineAmount" type="number" min={0} step="0.01" value={fineAmount} onChange={(e) => setFineAmount(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fineReason">Reason</Label>
              <Textarea id="fineReason" rows={2} value={fineReason} onChange={(e) => setFineReason(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFineOpen(false)}>Cancel</Button>
            <Button onClick={onAddFine} disabled={submitting}>{submitting && <Loader2 className="h-4 w-4 animate-spin" />} Add fine</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Record manual payment */}
      <Dialog open={manualOpen} onOpenChange={setManualOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Record a manual payment</DialogTitle>
            <DialogDescription>For cash, bank transfer, or cheque payments collected outside the gateway. A receipt is generated immediately.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Installment (optional)</Label>
              <Select value={manualInstallmentId} onValueChange={setManualInstallmentId}>
                <SelectTrigger><SelectValue placeholder="Apply to overall balance" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Apply to overall balance</SelectItem>
                  {fee.installments.map((i) => (
                    <SelectItem key={i.id} value={i.id}>Installment #{i.installmentNumber} — balance {formatCurrency(i.balanceDue, fee.currency)}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="manualAmount">Amount ({fee.currency})</Label>
                <Input id="manualAmount" type="number" min={0} step="0.01" value={manualAmount} onChange={(e) => setManualAmount(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label>Method</Label>
                <Select value={manualMethod} onValueChange={(v) => setManualMethod(v as PaymentMethod)}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {PAYMENT_METHODS.map((m) => <SelectItem key={m} value={m}>{m.replace("_", " ")}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="manualNote">Note (optional)</Label>
              <Textarea id="manualNote" rows={2} value={manualNote} onChange={(e) => setManualNote(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setManualOpen(false)}>Cancel</Button>
            <Button onClick={onRecordManual} disabled={submitting}>{submitting && <Loader2 className="h-4 w-4 animate-spin" />} Record payment</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Refund */}
      <Dialog open={!!refundTargetId} onOpenChange={(open) => !open && setRefundTargetId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Refund this payment</DialogTitle>
            <DialogDescription>Partial or full refunds are routed through the original gateway where applicable.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="refundAmount">Amount ({fee.currency})</Label>
              <Input id="refundAmount" type="number" min={0} step="0.01" value={refundAmount} onChange={(e) => setRefundAmount(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="refundReason">Reason</Label>
              <Textarea id="refundReason" rows={2} value={refundReason} onChange={(e) => setRefundReason(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRefundTargetId(null)}>Cancel</Button>
            <Button variant="destructive" onClick={onRefund} disabled={submitting}>{submitting && <Loader2 className="h-4 w-4 animate-spin" />} Request refund</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
function SummaryCard({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-xs font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className={highlight ? "text-xl font-bold text-primary" : "text-xl font-bold"}>{value}</p>
      </CardContent>
    </Card>
  );
}

