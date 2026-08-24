"use client";

import { useParams } from "next/navigation";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { placementsApi } from "@/lib/api/placements";
import { PlacementForm } from "@/components/placements/placement-form";

export default function EditPlacementPage() {
  const { id } = useParams<{ id: string }>();
  const { data: placement, isLoading, error, refetch } = useFetch(() => placementsApi.get(id), [id]);

  return (
    <div>
      <PageHeader title="Edit job drive" description="Update the details of this placement drive." />
      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading || !placement ? (
        <Skeleton className="h-96 w-full" />
      ) : (
        <PlacementForm placement={placement} />
      )}
    </div>
  );
}
