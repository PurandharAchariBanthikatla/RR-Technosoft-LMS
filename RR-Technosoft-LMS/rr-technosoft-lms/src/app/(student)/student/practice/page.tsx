"use client";

import { useState } from "react";
import Link from "next/link";
import { Search, Code2, CheckCircle2 } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { useDebounce } from "@/hooks/use-debounce";
import { practiceApi } from "@/lib/api/practice";
import { cn } from "@/lib/utils";
import { ProblemDifficulty } from "@/types";

const DIFFICULTY_CLASS: Record<ProblemDifficulty, string> = {
  EASY: "text-success",
  MEDIUM: "text-warning",
  HARD: "text-destructive",
};

export default function PracticePortalPage() {
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState("all");
  const debouncedSearch = useDebounce(search);

  const { data, isLoading, error, refetch } = useFetch(
    () =>
      practiceApi.list({
        search: debouncedSearch || undefined,
        difficulty: difficulty === "all" ? undefined : difficulty,
        page: 0,
        size: 30,
      }),
    [debouncedSearch, difficulty]
  );

  return (
    <div>
      <PageHeader title="Practice Portal" description="Sharpen your coding skills with hands-on problems." />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input placeholder="Search problems..." className="pl-8" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select value={difficulty} onValueChange={setDifficulty}>
          <SelectTrigger className="w-full sm:w-48"><SelectValue placeholder="Difficulty" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All difficulties</SelectItem>
            <SelectItem value="EASY">Easy</SelectItem>
            <SelectItem value="MEDIUM">Medium</SelectItem>
            <SelectItem value="HARD">Hard</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
        </div>
      ) : !data?.content || data.content.length === 0 ? (
        <EmptyState icon={Code2} title="No problems found" description="Try a different search or filter." />
      ) : (
        <div className="overflow-hidden rounded-lg border">
          {data.content.map((p, idx) => (
            <Link
              key={p.id}
              href={`/student/practice/${p.id}`}
              className={cn(
                "flex items-center justify-between px-4 py-3 text-sm transition-colors hover:bg-muted/50",
                idx !== 0 && "border-t"
              )}
            >
              <div className="flex items-center gap-3">
                {p.solvedByMe ? <CheckCircle2 className="h-4 w-4 text-success" /> : <span className="h-4 w-4" />}
                <span className="font-medium">{p.title}</span>
                <Badge variant="outline" className="text-[11px]">{p.topic}</Badge>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-xs text-muted-foreground">{p.successRate}% success</span>
                <span className={cn("text-xs font-semibold", DIFFICULTY_CLASS[p.difficulty])}>{p.difficulty}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
