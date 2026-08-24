"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, Search, Trash2, Pencil, Eye } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { DataTable, type Column } from "@/components/shared/data-table";
import { ErrorState } from "@/components/shared/error-state";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useFetch } from "@/hooks/use-fetch";
import { useDebounce } from "@/hooks/use-debounce";
import { coursesApi } from "@/lib/api/courses";
import { extractErrorMessage } from "@/lib/api/client";
import { Course } from "@/types";
import { toast } from "sonner";

export default function AdminCoursesPage() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>("all");
  const [deleteTarget, setDeleteTarget] = useState<Course | null>(null);
  const [deleting, setDeleting] = useState(false);
  const debouncedSearch = useDebounce(search);

  const { data, isLoading, error, refetch } = useFetch(
    () =>
      coursesApi.list({
        search: debouncedSearch || undefined,
        status: status === "all" ? undefined : status,
        page: 0,
        size: 20,
      }),
    [debouncedSearch, status]
  );

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await coursesApi.remove(deleteTarget.id);
      toast.success(`"${deleteTarget.title}" was deleted`);
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<Course>[] = [
    {
      key: "title",
      header: "Course",
      render: (c) => (
        <div>
          <p className="font-medium">{c.title}</p>
          <p className="text-xs text-muted-foreground">{c.category} · {c.moduleCount} modules</p>
        </div>
      ),
    },
    { key: "instructor", header: "Instructor", render: (c) => c.instructorName },
    { key: "level", header: "Level", render: (c) => c.level },
    { key: "students", header: "Enrolled", render: (c) => c.studentsEnrolled },
    { key: "status", header: "Status", render: (c) => <StatusBadge status={c.status} /> },
    {
      key: "actions",
      header: "",
      className: "text-right",
      render: (c) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon" asChild>
            <Link href={`/admin/courses/${c.id}`}><Eye className="h-4 w-4" /></Link>
          </Button>
          <Button variant="ghost" size="icon" asChild>
            <Link href={`/admin/courses/${c.id}?edit=1`}><Pencil className="h-4 w-4" /></Link>
          </Button>
          <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(c)}>
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Courses"
        description="Manage the full course catalog offered on RR TECHNOSOFT."
        actions={
          <Button asChild className="gap-2">
            <Link href="/admin/courses/new">
              <Plus className="h-4 w-4" /> New course
            </Link>
          </Button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search courses by title..."
            className="pl-8"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-full sm:w-48">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            <SelectItem value="DRAFT">Draft</SelectItem>
            <SelectItem value="PUBLISHED">Published</SelectItem>
            <SelectItem value="ARCHIVED">Archived</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : (
        <DataTable
          columns={columns}
          data={data?.content ?? []}
          isLoading={isLoading}
          rowKey={(c) => c.id}
          emptyTitle="No courses yet"
          emptyDescription="Get started by creating your first course."
        />
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Delete this course?"
        description={`"${deleteTarget?.title}" will be permanently removed along with its modules and lessons.`}
        confirmLabel="Delete course"
        destructive
        loading={deleting}
        onConfirm={handleDelete}
      />
    </div>
  );
}
