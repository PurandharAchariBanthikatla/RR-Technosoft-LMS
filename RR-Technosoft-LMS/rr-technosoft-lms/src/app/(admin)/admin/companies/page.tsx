"use client";

import { useState } from "react";
import { Building2, Plus, Pencil, Ban } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useFetch } from "@/hooks/use-fetch";
import { companiesApi } from "@/lib/api/companies";
import { extractErrorMessage } from "@/lib/api/client";
import { Company } from "@/types";
import { CompanyFormDialog } from "@/components/placements/company-form-dialog";

export default function AdminCompaniesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => companiesApi.list({ page: 0, size: 50 }), []);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Company | undefined>(undefined);
  const [deactivating, setDeactivating] = useState<Company | null>(null);
  const [busy, setBusy] = useState(false);

  function openCreate() {
    setEditing(undefined);
    setFormOpen(true);
  }

  function openEdit(company: Company) {
    setEditing(company);
    setFormOpen(true);
  }

  async function handleDeactivate() {
    if (!deactivating) return;
    setBusy(true);
    try {
      await companiesApi.remove(deactivating.id);
      toast.success("Company deactivated");
      setDeactivating(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const columns: Column<Company>[] = [
    {
      key: "name",
      header: "Company",
      render: (c) => (
        <div className="flex items-center gap-2">
          <Building2 className="h-4 w-4 text-primary" />
          <div>
            <p className="font-medium">{c.name}</p>
            {c.industry && <p className="text-xs text-muted-foreground">{c.industry}</p>}
          </div>
        </div>
      ),
    },
    { key: "contact", header: "Contact", render: (c) => c.contactPersonName ?? c.contactEmail ?? "—" },
    { key: "drives", header: "Active drives", render: (c) => c.activeDriveCount },
    {
      key: "status",
      header: "Status",
      render: (c) => <Badge variant={c.isActive ? "success" : "outline"}>{c.isActive ? "Active" : "Inactive"}</Badge>,
    },
    {
      key: "actions",
      header: "",
      render: (c) => (
        <div className="flex justify-end gap-2">
          <Button size="icon" variant="ghost" onClick={() => openEdit(c)}>
            <Pencil className="h-4 w-4" />
          </Button>
          {c.isActive && (
            <Button size="icon" variant="ghost" onClick={() => setDeactivating(c)}>
              <Ban className="h-4 w-4" />
            </Button>
          )}
        </div>
      ),
      className: "text-right",
    },
  ];

  return (
    <div>
      <PageHeader
        title="Companies"
        description="Manage the companies your placement drives are posted for."
        actions={
          <Button className="gap-2" onClick={openCreate}>
            <Plus className="h-4 w-4" /> New company
          </Button>
        }
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.content ?? []}
          isLoading={isLoading}
          rowKey={(c) => c.id}
          emptyTitle="No companies yet"
          emptyDescription="Add a company before posting a job drive for it."
        />
      )}

      <CompanyFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        company={editing}
        onSaved={() => {
          setFormOpen(false);
          refetch();
        }}
      />

      <ConfirmDialog
        open={!!deactivating}
        onOpenChange={(open) => !open && setDeactivating(null)}
        title="Deactivate company?"
        description={`${deactivating?.name} will no longer be selectable for new job drives.`}
        confirmLabel="Deactivate"
        destructive
        loading={busy}
        onConfirm={handleDeactivate}
      />
    </div>
  );
}
