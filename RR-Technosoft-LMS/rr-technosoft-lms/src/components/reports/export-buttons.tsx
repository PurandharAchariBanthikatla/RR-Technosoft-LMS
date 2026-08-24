"use client";

import { useState } from "react";
import { FileSpreadsheet, FileText, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { extractErrorMessage } from "@/lib/api/client";
import { toast } from "sonner";

interface ExportButtonsProps {
  onExportExcel: () => Promise<void>;
  onExportPdf: () => Promise<void>;
}

/** Pair of "Export Excel" / "Export PDF" buttons — used on every Reports & Analytics page. */
export function ExportButtons({ onExportExcel, onExportPdf }: ExportButtonsProps) {
  const [busy, setBusy] = useState<"excel" | "pdf" | null>(null);

  async function run(kind: "excel" | "pdf", fn: () => Promise<void>) {
    setBusy(kind);
    try {
      await fn();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="flex gap-2">
      <Button variant="outline" size="sm" className="gap-2" disabled={busy !== null} onClick={() => run("excel", onExportExcel)}>
        {busy === "excel" ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileSpreadsheet className="h-4 w-4" />}
        Export Excel
      </Button>
      <Button variant="outline" size="sm" className="gap-2" disabled={busy !== null} onClick={() => run("pdf", onExportPdf)}>
        {busy === "pdf" ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}
        Export PDF
      </Button>
    </div>
  );
}
