import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

interface ReportPaginationProps {
  page: number; // zero-based, matches Spring Data Page.number
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

export function ReportPagination({ page, totalPages, totalElements, onPageChange }: ReportPaginationProps) {
  if (totalElements === 0) return null;

  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <p className="text-muted-foreground">
        Page {page + 1} of {Math.max(totalPages, 1)} · {totalElements} record{totalElements === 1 ? "" : "s"}
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)} className="gap-1">
          <ChevronLeft className="h-4 w-4" /> Prev
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          className="gap-1"
        >
          Next <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
