"use client";

import { Award, Plus } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { certificatesApi } from "@/lib/api/certificates";
import { formatDate } from "@/lib/utils";
import { Certificate } from "@/types";

export default function AdminCertificatesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => certificatesApi.list({ page: 0, size: 20 }), []);

  const columns: Column<Certificate>[] = [
    {
      key: "student",
      header: "Student",
      render: (c) => (
        <div className="flex items-center gap-2">
          <Award className="h-4 w-4 text-primary" />
          {c.studentName}
        </div>
      ),
    },
    { key: "course", header: "Course", render: (c) => c.courseTitle },
    { key: "issued", header: "Issued on", render: (c) => formatDate(c.issuedAt) },
    { key: "code", header: "Verification code", render: (c) => <code className="text-xs">{c.verificationCode}</code> },
  ];

  return (
    <div>
      <PageHeader
        title="Certificates"
        description="Issue and manage completion certificates for students."
        actions={<Button className="gap-2"><Plus className="h-4 w-4" /> Issue certificate</Button>}
      />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable columns={columns} data={data?.content ?? []} isLoading={isLoading} rowKey={(c) => c.id} emptyTitle="No certificates issued yet" />
      )}
    </div>
  );
}
