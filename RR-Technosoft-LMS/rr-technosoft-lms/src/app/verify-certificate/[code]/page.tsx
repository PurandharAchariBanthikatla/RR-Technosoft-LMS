"use client";

import { useParams } from "next/navigation";
import { CheckCircle2, GraduationCap, XCircle } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { certificatesApi } from "@/lib/api/certificates";
import { formatDate } from "@/lib/utils";

/**
 * Public page — no auth required (see middleware.ts PUBLIC_PATHS). This is
 * what a certificate's embedded QR code links to, and what anyone (an
 * employer, a recruiter) can open directly by code to confirm a certificate
 * is genuine.
 */
export default function VerifyCertificatePage() {
  const params = useParams<{ code: string }>();
  const { data, isLoading, error } = useFetch(() => certificatesApi.verify(params.code), [params.code]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 px-6 py-12">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <GraduationCap className="h-5 w-5" />
          </span>
          <span className="font-display text-lg font-bold">RR TECHNOSOFT</span>
        </div>

        <Card>
          <CardContent className="space-y-4 p-6 text-center">
            {isLoading ? (
              <div className="space-y-3">
                <Skeleton className="mx-auto h-10 w-10 rounded-full" />
                <Skeleton className="mx-auto h-5 w-3/4" />
                <Skeleton className="mx-auto h-4 w-1/2" />
              </div>
            ) : error || !data ? (
              <>
                <XCircle className="mx-auto h-10 w-10 text-destructive" />
                <p className="font-display text-lg font-semibold">Certificate not found</p>
                <p className="text-sm text-muted-foreground">
                  We couldn&apos;t verify a certificate with code <code>{params.code}</code>. Double-check the
                  code and try again.
                </p>
              </>
            ) : (
              <>
                <CheckCircle2 className="mx-auto h-10 w-10 text-success" />
                <p className="font-display text-lg font-semibold">Certificate verified</p>
                <div className="space-y-1 text-sm">
                  <p>
                    <span className="text-muted-foreground">Awarded to</span>{" "}
                    <span className="font-medium">{data.studentName}</span>
                  </p>
                  <p>
                    <span className="text-muted-foreground">Course</span>{" "}
                    <span className="font-medium">{data.courseTitle}</span>
                  </p>
                  <p>
                    <span className="text-muted-foreground">Issued on</span>{" "}
                    <span className="font-medium">{formatDate(data.issuedAt)}</span>
                  </p>
                  <p className="pt-2 text-xs text-muted-foreground">
                    Code: <code>{data.verificationCode}</code>
                  </p>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
