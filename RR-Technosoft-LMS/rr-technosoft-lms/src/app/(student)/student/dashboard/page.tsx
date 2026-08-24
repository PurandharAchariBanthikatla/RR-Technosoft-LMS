"use client";

import Link from "next/link";
import { BookOpen, CalendarCheck, ClipboardList, Award, Video, ArrowRight } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { ProgressRing } from "@/components/dashboard/progress-ring";
import { useFetch } from "@/hooks/use-fetch";
import { dashboardApi } from "@/lib/api/dashboard";
import { useAuthStore } from "@/store/auth-store";
import { formatDate, formatTime } from "@/lib/utils";

export default function StudentDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => dashboardApi.student(), []);

  return (
    <div>
      <PageHeader
        title={`Hey${user?.name ? `, ${user.name.split(" ")[0]}` : ""} 👋`}
        description="Here's a snapshot of your learning journey."
      />

      {error && <ErrorState message={error} onRetry={refetch} />}

      {!error && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {isLoading ? (
                Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-lg" />)
              ) : (
                <>
                  <StatCard label="Enrolled Courses" value={data?.enrolledCourses ?? 0} icon={BookOpen} />
                  <StatCard label="Completed Courses" value={data?.completedCourses ?? 0} icon={Award} accent="success" />
                  <StatCard label="Attendance" value={`${data?.attendancePercentage ?? 0}%`} icon={CalendarCheck} />
                  <StatCard label="Pending Assignments" value={data?.pendingAssignments ?? 0} icon={ClipboardList} accent="warning" />
                </>
              )}
            </div>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>Upcoming live classes</CardTitle>
                  <CardDescription>Don&apos;t miss your next session</CardDescription>
                </div>
                <Button variant="ghost" size="sm" asChild className="gap-1">
                  <Link href="/student/live-classes">View all <ArrowRight className="h-3.5 w-3.5" /></Link>
                </Button>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
                  </div>
                ) : !data?.upcomingClasses || data.upcomingClasses.length === 0 ? (
                  <EmptyState icon={Video} title="No upcoming classes" description="New sessions will appear here once scheduled." />
                ) : (
                  <div className="divide-y">
                    {data.upcomingClasses.map((c) => (
                      <div key={c.id} className="flex items-center justify-between py-3">
                        <div>
                          <p className="font-medium">{c.title}</p>
                          <p className="text-xs text-muted-foreground">{c.courseTitle} · {c.instructorName}</p>
                        </div>
                        <div className="text-right text-sm">
                          <p className="font-medium">{formatDate(c.startTime)}</p>
                          <p className="text-xs text-muted-foreground">{formatTime(c.startTime)}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <Card className="flex flex-col items-center justify-center gap-3 py-8">
            <CardTitle className="text-center text-base">Overall progress</CardTitle>
            {isLoading ? (
              <Skeleton className="h-32 w-32 rounded-full" />
            ) : (
              <ProgressRing value={data?.overallProgress ?? 0} label="completed" />
            )}
            <p className="text-center text-xs text-muted-foreground px-6">
              Across all your enrolled courses, modules and lessons.
            </p>
            <Button variant="outline" size="sm" asChild>
              <Link href="/student/courses">Continue learning</Link>
            </Button>
          </Card>
        </div>
      )}
    </div>
  );
}
