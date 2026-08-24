import { PageHeader } from "@/components/shared/page-header";
import { CourseForm } from "@/components/courses/course-form";

export default function NewCoursePage() {
  return (
    <div>
      <PageHeader title="Create a new course" description="Fill in the details below to add a course to the catalog." />
      <CourseForm />
    </div>
  );
}
