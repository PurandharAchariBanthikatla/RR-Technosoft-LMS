"use client";

import { CheckCircle2, Circle, ListTodo } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { dailyTasksApi } from "@/lib/api/assignments";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { extractErrorMessage } from "@/lib/api/client";

export default function StudentDailyTasksPage() {
  const { data, isLoading, error, refetch } = useFetch(() => dailyTasksApi.list(), []);

  async function handleToggle(id: string, completed: boolean) {
    try {
      await dailyTasksApi.toggle(id, completed);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  return (
    <div>
      <PageHeader title="Daily Tasks" description="Small, focused tasks to keep your daily learning streak alive." />

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16 w-full" />)}
        </div>
      ) : !data || data.length === 0 ? (
        <EmptyState icon={ListTodo} title="No tasks for today" description="Check back tomorrow for your next set of daily tasks." />
      ) : (
        <div className="space-y-2">
          {data.map((task) => (
            <Card key={task.id} className={cn(task.completed && "bg-muted/40")}>
              <CardContent className="flex items-start gap-3 p-4">
                <button onClick={() => handleToggle(task.id, !task.completed)} aria-label="Toggle task">
                  {task.completed ? (
                    <CheckCircle2 className="mt-0.5 h-5 w-5 text-success" />
                  ) : (
                    <Circle className="mt-0.5 h-5 w-5 text-muted-foreground" />
                  )}
                </button>
                <div>
                  <p className={cn("font-medium", task.completed && "text-muted-foreground line-through")}>{task.title}</p>
                  <p className="text-sm text-muted-foreground">{task.description}</p>
                  {task.courseTitle && <p className="mt-1 text-xs text-primary">{task.courseTitle}</p>}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
