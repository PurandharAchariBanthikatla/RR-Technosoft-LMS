"use client";

import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { getAdminNav } from "@/lib/nav-config";
import { Role } from "@/lib/constants";
import { useAuth } from "@/hooks/use-auth";
import { PageLoader } from "@/components/shared/loading-spinner";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth([Role.SUPER_ADMIN, Role.ADMIN]);

  if (isLoading) return <PageLoader />;

  return (
    <div className="flex min-h-screen">
      <Sidebar items={getAdminNav(user?.role)} portalLabel="Admin Portal" />
      <div className="flex min-h-screen flex-1 flex-col">
        <Topbar />
        <main className="flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
