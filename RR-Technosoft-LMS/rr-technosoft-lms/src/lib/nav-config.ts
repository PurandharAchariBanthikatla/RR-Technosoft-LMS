import {
  LayoutDashboard,
  BookOpen,
  Layers,
  Users,
  ClipboardList,
  FileCheck2,
  HelpCircle,
  Video,
  CalendarCheck,
  Award,
  Briefcase,
  Building2,
  FolderOpen,
  Code2,
  ListTodo,
  ShieldCheck,
  Settings,
  BarChart3,
  Wallet,
  Receipt,
  Bot,
  type LucideIcon,
} from "lucide-react";
import { Role } from "@/lib/constants";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
}

export const adminNav: NavItem[] = [
  { label: "Dashboard", href: "/admin/dashboard", icon: LayoutDashboard },
  { label: "Courses", href: "/admin/courses", icon: BookOpen },
  { label: "Students", href: "/admin/students", icon: Users },
  { label: "Enrollments", href: "/admin/enrollments", icon: Layers },
  { label: "Assignments", href: "/admin/assignments", icon: ClipboardList },
  { label: "Quizzes", href: "/admin/quizzes", icon: HelpCircle },
  { label: "Live Classes", href: "/admin/live-classes", icon: Video },
  { label: "Attendance", href: "/admin/attendance", icon: CalendarCheck },
  { label: "Certificates", href: "/admin/certificates", icon: Award },
  { label: "Learning Resources", href: "/admin/learning-resources", icon: FolderOpen },
  { label: "Video Library", href: "/admin/videos", icon: Video },
  { label: "Companies", href: "/admin/companies", icon: Building2 },
  { label: "Placements", href: "/admin/placements", icon: Briefcase },
  { label: "Fee Structures", href: "/admin/finance/fee-structures", icon: Wallet },
  { label: "Student Fees", href: "/admin/finance/student-fees", icon: Receipt },
  { label: "Payments", href: "/admin/finance/payments", icon: Receipt },
  { label: "Finance Reports", href: "/admin/finance/reports", icon: BarChart3 },
  { label: "Reports & Analytics", href: "/admin/reports", icon: BarChart3 },
];

/** SUPER_ADMIN-only items, matching /admins being locked down on the backend. */
const superAdminOnlyNav: NavItem[] = [
  { label: "Admins", href: "/admin/admins", icon: ShieldCheck },
  { label: "Administration", href: "/admin/settings", icon: Settings },
];

export function getAdminNav(role?: Role): NavItem[] {
  return role === Role.SUPER_ADMIN ? [...adminNav, ...superAdminOnlyNav] : adminNav;
}

export const studentNav: NavItem[] = [
  { label: "Dashboard", href: "/student/dashboard", icon: LayoutDashboard },
  { label: "My Courses", href: "/student/courses", icon: BookOpen },
  { label: "Daily Tasks", href: "/student/daily-tasks", icon: ListTodo },
  { label: "Assignments", href: "/student/assignments", icon: ClipboardList },
  { label: "Quizzes", href: "/student/quizzes", icon: HelpCircle },
  { label: "Live Classes", href: "/student/live-classes", icon: Video },
  { label: "Attendance", href: "/student/attendance", icon: CalendarCheck },
  { label: "Practice Portal", href: "/student/practice", icon: Code2 },
  { label: "AI Assistant", href: "/student/chatbot", icon: Bot },
  { label: "Learning Resources", href: "/student/learning-resources", icon: FolderOpen },
  { label: "Video Library", href: "/student/videos", icon: Video },
  { label: "Certificates", href: "/student/certificates", icon: Award },
  { label: "Placements", href: "/student/placements", icon: Briefcase },
  { label: "My Fees", href: "/student/fees", icon: Wallet },
  { label: "Payment History", href: "/student/payments", icon: Receipt },
  { label: "Submissions", href: "/student/assignments?tab=submitted", icon: FileCheck2 },
];
