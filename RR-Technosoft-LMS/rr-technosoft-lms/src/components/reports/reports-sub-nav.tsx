"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const items = [
  { href: "/admin/reports", label: "Dashboard" },
  { href: "/admin/reports/students", label: "Students" },
  { href: "/admin/reports/faculty", label: "Faculty" },
  { href: "/admin/reports/attendance", label: "Attendance" },
  { href: "/admin/reports/assignments", label: "Assignments" },
  { href: "/admin/reports/revenue", label: "Revenue" },
];

export function ReportsSubNav() {
  const pathname = usePathname();

  return (
    <div className="mb-6 flex flex-wrap gap-1 border-b">
      {items.map((item) => {
        const active = pathname === item.href;
        return (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "-mb-px rounded-t-md border-b-2 px-3 py-2 text-sm font-medium transition-colors",
              active
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </div>
  );
}
