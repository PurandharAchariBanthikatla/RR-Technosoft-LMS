import { PageHeader } from "@/components/shared/page-header";
import { PlacementForm } from "@/components/placements/placement-form";

export default function NewPlacementPage() {
  return (
    <div>
      <PageHeader title="Post a new job drive" description="Fill in the role details below to open a new placement drive." />
      <PlacementForm />
    </div>
  );
}
