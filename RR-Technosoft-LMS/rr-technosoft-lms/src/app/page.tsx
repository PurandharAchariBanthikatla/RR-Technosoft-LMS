"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { ROLE_HOME } from "@/lib/constants";
import { PageLoader } from "@/components/shared/loading-spinner";

export default function RootPage() {
  const router = useRouter();
  const { user, isAuthenticated, isHydrated } = useAuthStore();

  useEffect(() => {
    if (!isHydrated) return;
    if (isAuthenticated && user) {
      router.replace(ROLE_HOME[user.role]);
    } else {
      router.replace("/login");
    }
  }, [isHydrated, isAuthenticated, user, router]);

  return <PageLoader />;
}
