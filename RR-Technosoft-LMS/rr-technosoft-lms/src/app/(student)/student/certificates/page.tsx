"use client";

import { Award, Download, ShieldCheck } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { certificatesApi } from "@/lib/api/certificates";
import { formatDate } from "@/lib/utils";

export default function StudentCertificatesPage() {
  const { data, isLoading, error, refetch } = useFetch(() => certificatesApi.mine(), []);

  return (
    <div>
      <PageHeader title="Certificates" description="Download and share your earned certificates." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {Array.from({ length: 2 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-lg" />)}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState icon={Award} title="No certificates yet" description="Complete a course to earn your first certificate." />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {data.map((c) => (
            <Card key={c.id} className="overflow-hidden">
              <div className="flex h-28 items-center justify-center bg-gradient-to-br from-brand-ink to-neutral-800">
                <Award className="h-10 w-10 text-primary" />
              </div>
              <CardContent className="space-y-3 p-5">
                <div>
                  <p className="font-display font-semibold">{c.courseTitle}</p>
                  <p className="text-sm text-muted-foreground">Issued on {formatDate(c.issuedAt)}</p>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <ShieldCheck className="h-3.5 w-3.5" /> Code: <code>{c.verificationCode}</code>
                </div>
                <Button size="sm" variant="outline" className="w-full gap-2" asChild>
                  <a href={c.certificateUrl} target="_blank" rel="noreferrer">
                    <Download className="h-4 w-4" /> Download certificate
                  </a>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
