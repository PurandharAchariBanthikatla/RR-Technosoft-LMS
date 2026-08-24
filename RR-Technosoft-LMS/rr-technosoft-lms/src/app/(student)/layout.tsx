"use client";

import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { studentNav } from "@/lib/nav-config";
import { Role } from "@/lib/constants";
import { useAuth } from "@/hooks/use-auth";
import { PageLoader } from "@/components/shared/loading-spinner";

export default function StudentLayout({ children }: { children: React.ReactNode }) {
  const { isLoading } = useAuth([Role.STUDENT]);

  if (isLoading) return <PageLoader />;

  return (
    <div className="flex min-h-screen">
      <Sidebar items={studentNav} portalLabel="Student Portal" />
      <div className="flex min-h-screen flex-1 flex-col">
        <Topbar />
        <main className="flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
