"use client";

import { useState } from "react";
import { Search } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { CourseCard } from "@/components/courses/course-card";
import { useFetch } from "@/hooks/use-fetch";
import { useDebounce } from "@/hooks/use-debounce";
import { enrollmentsApi } from "@/lib/api/enrollments";
import { BookOpen } from "lucide-react";

export default function StudentCoursesPage() {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search);
  const { data, isLoading, error, refetch } = useFetch(() => enrollmentsApi.mine(), []);

  const filtered = (data ?? []).filter((e) =>
    e.courseTitle.toLowerCase().includes(debouncedSearch.toLowerCase())
  );

  return (
    <div>
      <PageHeader title="My Courses" description="Pick up right where you left off." />

      <div className="relative mb-5 max-w-sm">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input placeholder="Search your courses..." className="pl-8" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {error ? (
        <ErrorState message={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-64 rounded-lg" />)}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState icon={BookOpen} title="No courses yet" description="Once you enroll in a course, it will show up here." />
      ) : (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((e) => (
            <CourseCard
              key={e.id}
              course={{
                id: e.courseId,
                title: e.courseTitle,
                slug: e.courseId,
                description: "Continue where you left off in this course.",
                category: "",
                level: "BEGINNER",
                status: "PUBLISHED",
                durationWeeks: 0,
                instructorName: "",
                price: 0,
                studentsEnrolled: 0,
                moduleCount: 0,
                createdAt: e.enrolledAt,
              }}
              progress={e.progress}
              href={`/student/courses/${e.courseId}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
