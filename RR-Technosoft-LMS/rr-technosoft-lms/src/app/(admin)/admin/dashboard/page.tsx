"use client";

import { Users, BookOpen, Layers, Wallet, Video, ClipboardList } from "lucide-react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { useFetch } from "@/hooks/use-fetch";
import { dashboardApi } from "@/lib/api/dashboard";
import { useAuthStore } from "@/store/auth-store";

export default function AdminDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const { data, isLoading, error, refetch } = useFetch(() => dashboardApi.admin(), []);

  return (
    <div>
      <PageHeader
        title={`Welcome back${user?.name ? `, ${user.name.split(" ")[0]}` : ""}`}
        description="Here's what's happening across RR TECHNOSOFT today."
      />

      {error && <ErrorState message={error} onRetry={refetch} />}

      {!error && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {isLoading
              ? Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-lg" />)
              : [
                  { label: "Total Students", value: data?.totalStudents ?? 0, icon: Users },
                  { label: "Active Courses", value: data?.totalCourses ?? 0, icon: BookOpen },
                  { label: "Active Enrollments", value: data?.activeEnrollments ?? 0, icon: Layers },
                  {
                    label: "Total Revenue",
                    value: `₹${(data?.totalRevenue ?? 0).toLocaleString("en-IN")}`,
                    icon: Wallet,
                  },
                  { label: "Live Classes This Week", value: data?.upcomingLiveClasses ?? 0, icon: Video },
                  { label: "Pending Assignments", value: data?.pendingAssignments ?? 0, icon: ClipboardList },
                ].map((s) => <StatCard key={s.label} {...s} />)}
          </div>

          <Card className="mt-6">
            <CardHeader>
              <CardTitle>Student growth</CardTitle>
              <CardDescription>New enrollments over the last 6 months</CardDescription>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <Skeleton className="h-72 w-full" />
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <AreaChart data={data?.studentGrowth ?? []}>
                    <defs>
                      <linearGradient id="colorStudents" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#E31E24" stopOpacity={0.35} />
                        <stop offset="95%" stopColor="#E31E24" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border" />
                    <XAxis dataKey="month" tickLine={false} axisLine={false} fontSize={12} />
                    <YAxis tickLine={false} axisLine={false} fontSize={12} />
                    <Tooltip
                      contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }}
                    />
                    <Area type="monotone" dataKey="students" stroke="#E31E24" strokeWidth={2} fill="url(#colorStudents)" />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
