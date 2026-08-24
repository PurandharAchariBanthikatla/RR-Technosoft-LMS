"use client";

import { useState } from "react";
import { Loader2, FileDown, Receipt as ReceiptIcon } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { useFetch } from "@/hooks/use-fetch";
import { paymentsApi, receiptsApi, openPdfInNewTab } from "@/lib/api/finance";
import { extractErrorMessage } from "@/lib/api/client";
import { formatCurrency, formatDate } from "@/lib/utils";
import { Payment } from "@/types";

export default function StudentPaymentsPage() {
  const { data: payments, isLoading, error, refetch } = useFetch(() => paymentsApi.mine(), []);
  const { data: receipts } = useFetch(() => receiptsApi.mine(), []);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  async function handleDownloadReceipt(paymentId: string) {
    const receipt = receipts?.find((r) => r.paymentId === paymentId);
    if (!receipt) {
      toast.error("Receipt not available for this payment yet");
      return;
    }
    setDownloadingId(paymentId);
    try {
      await openPdfInNewTab(receiptsApi.pdfUrl(receipt.id), `${receipt.receiptNumber}.pdf`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDownloadingId(null);
    }
  }

  const columns: Column<Payment>[] = [
    { key: "amount", header: "Amount", render: (p) => formatCurrency(p.amount, p.currency) },
    { key: "method", header: "Method", render: (p) => p.method ?? "—" },
    { key: "status", header: "Status", render: (p) => <StatusBadge status={p.status} /> },
    { key: "refunded", header: "Refunded", render: (p) => (p.refundedAmount > 0 ? formatCurrency(p.refundedAmount, p.currency) : "—") },
    { key: "date", header: "Date", render: (p) => (p.paidAt ? formatDate(p.paidAt) : formatDate(p.createdAt)) },
    {
      key: "receipt",
      header: "",
      className: "text-right",
      render: (p) => {
        const hasReceipt = receipts?.some((r) => r.paymentId === p.id);
        if (!hasReceipt) return null;
        return (
          <div className="flex justify-end">
            <Button variant="outline" size="sm" className="gap-1.5" disabled={downloadingId === p.id} onClick={() => handleDownloadReceipt(p.id)}>
              {downloadingId === p.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileDown className="h-4 w-4" />}
              Receipt
            </Button>
          </div>
        );
      },
    },
  ];

  return (
    <div>
      <PageHeader title="Payment History" description="Every payment you've made, with downloadable receipts." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={payments ?? []}
          isLoading={isLoading}
          rowKey={(p) => p.id}
          emptyTitle="No payments yet"
          emptyDescription="Payments you make from the My Fees page will show up here."
        />
      )}

      {!error && (payments?.length ?? 0) > 0 && (receipts?.length ?? 0) === 0 && (
        <p className="mt-4 flex items-center gap-1.5 text-xs text-muted-foreground">
          <ReceiptIcon className="h-3.5 w-3.5" /> Receipts appear once a payment is confirmed successful.
        </p>
      )}
    </div>
  );
}
