"use client";

import Link from "next/link";
import {
  ShieldAlert,
  KeyRound,
  Building2,
  Database,
  SlidersHorizontal,
  Lock,
  Mail,
  HardDriveDownload,
  ToggleLeft,
  ChevronRight,
  History,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { useAuthStore } from "@/store/auth-store";
import { Role } from "@/lib/constants";

const sections = [
  {
    href: "/admin/settings/permissions",
    icon: KeyRound,
    title: "Permission Matrix",
    description: "Grant or revoke role-based permissions for Admin and Student — no redeploy needed.",
  },
  {
    href: "/admin/settings/organization",
    icon: Building2,
    title: "Organization Profile",
    description: "Branding, contact details, timezone and locale shown across the platform.",
  },
  {
    href: "/admin/settings/master-data",
    icon: Database,
    title: "Master Data",
    description: "Maintain lookup lists — departments, designations, skill tags, course categories.",
  },
  {
    href: "/admin/settings/system",
    icon: SlidersHorizontal,
    title: "System Settings",
    description: "Platform-wide preferences: page sizes, default thresholds, maintenance messaging.",
  },
  {
    href: "/admin/settings/security",
    icon: Lock,
    title: "Security Settings",
    description: "Password policy, login lockout, session timeout and MFA requirements.",
  },
  {
    href: "/admin/settings/notifications",
    icon: Mail,
    title: "Notifications & Email",
    description: "SMTP configuration and default notification channel preferences.",
  },
  {
    href: "/admin/settings/backup",
    icon: HardDriveDownload,
    title: "Backup & Restore",
    description: "Backup schedule, retention and storage — trigger a run and review history.",
  },
  {
    href: "/admin/settings/feature-toggles",
    icon: ToggleLeft,
    title: "Feature Toggles",
    description: "Turn platform features on or off for everyone, instantly.",
  },
  {
    href: "/admin/settings/audit-logs",
    icon: History,
    title: "Audit Log History",
    description: "Searchable record of every important user and administrative action, with timestamps.",
  },
];

export default function AdministrationHubPage() {
  const currentUser = useAuthStore((s) => s.user);

  // Defense in depth: every write on these screens is also blocked at the
  // backend (SecurityConfig: /administration/**, /system-settings/** ->
  // hasRole('SUPER_ADMIN')), so a non-super-admin's requests would 403 even
  // if this check were bypassed client-side.
  if (currentUser && currentUser.role !== Role.SUPER_ADMIN) {
    return (
      <EmptyState
        icon={ShieldAlert}
        title="Super Admins only"
        description="Only a Super Admin can access platform administration."
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Administration"
        description="Platform-wide configuration — settings, permissions and master data."
      />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map((s) => (
          <Link key={s.href} href={s.href}>
            <Card className="h-full transition-colors hover:border-red-600/40 hover:bg-red-50/40">
              <CardHeader className="flex flex-row items-start justify-between gap-2 space-y-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-red-600/10">
                  <s.icon className="h-5 w-5 text-red-600" />
                </div>
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <CardTitle className="text-base">{s.title}</CardTitle>
                <CardDescription className="mt-1">{s.description}</CardDescription>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
