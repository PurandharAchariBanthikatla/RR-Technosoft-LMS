"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { coursesApi } from "@/lib/api/courses";

interface CourseFilterSelectProps {
  value: string | undefined;
  onChange: (courseId: string | undefined) => void;
  className?: string;
}

const ALL = "__all__";

/** Populates from the live course catalog — same data source the Courses module itself uses. */
export function CourseFilterSelect({ value, onChange, className }: CourseFilterSelectProps) {
  const { data } = useFetch(() => coursesApi.list({ page: 0, size: 200 }), []);

  return (
    <Select value={value ?? ALL} onValueChange={(v) => onChange(v === ALL ? undefined : v)}>
      <SelectTrigger className={className}>
        <SelectValue placeholder="All courses" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={ALL}>All courses</SelectItem>
        {(data?.content ?? []).map((c) => (
          <SelectItem key={c.id} value={c.id}>
            {c.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
