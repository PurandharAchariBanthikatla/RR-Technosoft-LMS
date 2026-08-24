"use client";

import { useState } from "react";
import { Loader2, FileDown, CreditCard, Wallet } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { Progress } from "@/components/ui/progress";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/store/auth-store";
import { studentFeesApi, paymentsApi, openPdfInNewTab } from "@/lib/api/finance";
import { openRazorpayCheckout } from "@/lib/razorpay";
import { extractErrorMessage } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/lib/utils";
import { StudentFeeInstallment } from "@/types";

export default function StudentFeesPage() {
  const { user } = useAuthStore();
  const { data: fees, isLoading, error, refetch } = useFetch(() => studentFeesApi.mine(), []);
  const [payingId, setPayingId] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  async function handlePay(studentFeeId: string, installment: StudentFeeInstallment | null, amount: number) {
    const key = installment?.id ?? studentFeeId;
    setPayingId(key);
    try {
      const order = await paymentsApi.initiate({
        studentFeeId,
        installmentId: installment?.id,
        amount,
      });
      const result = await openRazorpayCheckout(order, {
        studentName: user?.name,
        studentEmail: user?.email,
        description: installment ? `Installment #${installment.installmentNumber}` : "Fee payment",
      });
      await paymentsApi.verify({
        paymentId: order.paymentId,
        gatewayOrderId: result.razorpay_order_id,
        gatewayPaymentId: result.razorpay_payment_id,
        gatewaySignature: result.razorpay_signature,
      });
      toast.success("Payment successful — receipt generated");
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setPayingId(null);
    }
  }

  async function handleInvoice(studentFeeId: string) {
    setDownloadingId(studentFeeId);
    try {
      await openPdfInNewTab(studentFeesApi.invoicePdfUrl(studentFeeId), `invoice-${studentFeeId}.pdf`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDownloadingId(null);
    }
  }

  return (
    <div>
      <PageHeader title="My Fees" description="Track your fee balance, installments, and pay securely online." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-48 w-full" />
        </div>
      ) : (fees?.length ?? 0) === 0 ? (
        <EmptyState icon={Wallet} title="No fees assigned yet" description="Your fee record will appear here once the finance team assigns it." />
      ) : (
        <div className="space-y-6">
          {fees!.map((fee) => {
            const pct = fee.netPayable > 0 ? Math.min(100, Math.round((fee.amountPaid / fee.netPayable) * 100)) : 0;
            return (
              <Card key={fee.id}>
                <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <CardTitle className="text-base">{fee.courseTitle ?? "General fee"}</CardTitle>
                    <p className="text-xs text-muted-foreground">{fee.feeStructureName ?? "Custom plan"}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge status={fee.status} />
                    <Button
                      variant="outline" size="sm" className="gap-1.5"
                      disabled={downloadingId === fee.id}
                      onClick={() => handleInvoice(fee.id)}
                    >
                      {downloadingId === fee.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileDown className="h-4 w-4" />}
                      Invoice
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <Metric label="Net payable" value={formatCurrency(fee.netPayable, fee.currency)} />
                    <Metric label="Paid" value={formatCurrency(fee.amountPaid, fee.currency)} />
                    <Metric label="Balance due" value={formatCurrency(fee.balanceDue, fee.currency)} highlight={fee.balanceDue > 0} />
                    <div>
                      <p className="text-xs text-muted-foreground">Progress</p>
                      <div className="mt-1.5 flex items-center gap-2">
                        <Progress value={pct} className="h-1.5" />
                        <span className="w-9 text-xs text-muted-foreground">{pct}%</span>
                      </div>
                    </div>
                  </div>

                  {fee.balanceDue > 0 && fee.status !== "WAIVED" && fee.status !== "CANCELLED" && fee.installments.length === 0 && (
                    <Button
                      className="gap-1.5"
                      disabled={payingId === fee.id}
                      onClick={() => handlePay(fee.id, null, fee.balanceDue)}
                    >
                      {payingId === fee.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
                      Pay {formatCurrency(fee.balanceDue, fee.currency)}
                    </Button>
                  )}

                  {fee.installments.length > 0 && (
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>#</TableHead>
                          <TableHead>Amount</TableHead>
                          <TableHead>Due date</TableHead>
                          <TableHead>Balance</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead className="text-right">Action</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {fee.installments.map((i) => (
                          <TableRow key={i.id}>
                            <TableCell>{i.installmentNumber}</TableCell>
                            <TableCell>{formatCurrency(i.amount, fee.currency)}</TableCell>
                            <TableCell>{formatDate(i.dueDate)}</TableCell>
                            <TableCell>{formatCurrency(i.balanceDue, fee.currency)}</TableCell>
                            <TableCell><StatusBadge status={i.status} /></TableCell>
                            <TableCell className="text-right">
                              {i.balanceDue > 0 && i.status !== "WAIVED" && (
                                <Button
                                  size="sm" className="gap-1.5"
                                  disabled={payingId === i.id}
                                  onClick={() => handlePay(fee.id, i, i.balanceDue)}
                                >
                                  {payingId === i.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
                                  Pay
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}

function Metric({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={highlight ? "text-base font-semibold text-primary" : "text-base font-semibold"}>{value}</p>
    </div>
  );
}
