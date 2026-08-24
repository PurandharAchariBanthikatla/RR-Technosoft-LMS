"use client";

import {
  Users,
  BookOpen,
  Layers,
  Wallet,
  GraduationCap,
  CalendarCheck,
  TrendingUp,
  ClipboardCheck,
} from "lucide-react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";

import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { ReportsSubNav } from "@/components/reports/reports-sub-nav";
import { useFetch } from "@/hooks/use-fetch";
import { reportsApi } from "@/lib/api/reports";

const PIE_COLORS = ["#E31E24", "#F97316", "#EAB308", "#22C55E", "#0EA5E9", "#8B5CF6", "#EC4899"];

export default function ReportsDashboardPage() {
  const { data, isLoading, error, refetch } = useFetch(() => reportsApi.dashboard(), []);

  return (
    <div>
      <PageHeader
        title="Reports & Analytics"
        description="A live, filterable view of students, faculty, attendance, assignments and revenue across RR TECHNOSOFT."
      />
      <ReportsSubNav />

      {error && <ErrorState message={error} onRetry={refetch} />}

      {!error && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {isLoading
              ? Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-lg" />)
              : [
                  { label: "Total Students", value: data?.totalStudents ?? 0, icon: Users },
                  { label: "Total Courses", value: data?.totalCourses ?? 0, icon: BookOpen },
                  { label: "Active Enrollments", value: data?.activeEnrollments ?? 0, icon: Layers },
                  {
                    label: "Total Revenue",
                    value: `₹${(data?.totalRevenue ?? 0).toLocaleString("en-IN")}`,
                    icon: Wallet,
                  },
                  { label: "Faculty", value: data?.totalFaculty ?? 0, icon: GraduationCap },
                  {
                    label: "Avg Attendance",
                    value: `${(data?.avgAttendancePercentage ?? 0).toFixed(1)}%`,
                    icon: CalendarCheck,
                  },
                  {
                    label: "Avg Course Completion",
                    value: `${(data?.avgCourseCompletionPercentage ?? 0).toFixed(1)}%`,
                    icon: TrendingUp,
                  },
                  { label: "Pending Grading", value: data?.pendingAssignments ?? 0, icon: ClipboardCheck },
                ].map((s) => <StatCard key={s.label} {...s} />)}
          </div>

          <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Revenue trend</CardTitle>
                <CardDescription>Monthly revenue from paid enrollments, last 6 months</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <Skeleton className="h-72 w-full" />
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <AreaChart data={data?.revenueTrend ?? []}>
                      <defs>
                        <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#E31E24" stopOpacity={0.35} />
                          <stop offset="95%" stopColor="#E31E24" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border" />
                      <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                      <YAxis tickLine={false} axisLine={false} fontSize={12} />
                      <Tooltip
                        contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }}
                        formatter={(v: number) => [`₹${v.toLocaleString("en-IN")}`, "Revenue"]}
                      />
                      <Area type="monotone" dataKey="value" stroke="#E31E24" strokeWidth={2} fill="url(#colorRevenue)" />
                    </AreaChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Student growth</CardTitle>
                <CardDescription>New enrollments per month, last 6 months</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <Skeleton className="h-72 w-full" />
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={data?.studentGrowth ?? []}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border" />
                      <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                      <YAxis tickLine={false} axisLine={false} fontSize={12} />
                      <Tooltip contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }} />
                      <Bar dataKey="value" fill="#E31E24" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Attendance trend</CardTitle>
                <CardDescription>Average attendance %, last 6 months</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <Skeleton className="h-72 w-full" />
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <LineChart data={data?.attendanceTrend ?? []}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-border" />
                      <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                      <YAxis tickLine={false} axisLine={false} fontSize={12} domain={[0, 100]} />
                      <Tooltip
                        contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }}
                        formatter={(v: number) => [`${v}%`, "Attendance"]}
                      />
                      <Line type="monotone" dataKey="value" stroke="#0EA5E9" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Course distribution</CardTitle>
                <CardDescription>Enrollments by category</CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <Skeleton className="h-72 w-full" />
                ) : (data?.courseDistribution?.length ?? 0) === 0 ? (
                  <p className="py-16 text-center text-sm text-muted-foreground">No course data yet.</p>
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <PieChart>
                      <Pie
                        data={data?.courseDistribution ?? []}
                        dataKey="enrollmentCount"
                        nameKey="category"
                        innerRadius={55}
                        outerRadius={95}
                        paddingAngle={2}
                      >
                        {(data?.courseDistribution ?? []).map((_, i) => (
                          <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))", fontSize: 13 }} />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
