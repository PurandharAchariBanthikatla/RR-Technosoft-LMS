"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { jwtDecode } from "jwt-decode";
import { useAuthStore } from "@/store/auth-store";
import { ACCESS_TOKEN_KEY, Role, ROLE_HOME } from "@/lib/constants";
import { clearTokens } from "@/lib/api/client";

/** Claims issued by JwtService#generateAccessToken on the backend. */
interface AccessTokenClaims {
  sub: string;
  role: Role;
  email?: string;
  studentId?: string;
  fullName?: string;
  exp: number;
  iat: number;
  iss: string;
}

/**
 * Route guard for protected pages. There is no /auth/me endpoint on the backend —
 * the access token itself carries the role claim, so we decode it client-side to
 * verify the session is still valid and the user is allowed on this route. The
 * axios interceptor in lib/api/client.ts separately handles silent token refresh
 * for API calls made while the page is open.
 */
export function useAuth(requiredRoles?: Role[]) {
  const router = useRouter();
  const { user, isAuthenticated, isHydrated, setUser } = useAuthStore();
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    if (!isHydrated) return;

    if (!isAuthenticated || !user) {
      setChecking(false);
      router.replace("/login");
      return;
    }

    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) {
      setChecking(false);
      clearTokens();
      setUser(null);
      router.replace("/login");
      return;
    }

    try {
      const claims = jwtDecode<AccessTokenClaims>(token);
      const isExpired = claims.exp * 1000 < Date.now();

      if (isExpired) {
        // The request interceptor will attempt a refresh on the next API call;
        // for a page load with no in-flight request yet, send the user back to
        // sign in rather than showing a page with stale data.
        setChecking(false);
        clearTokens();
        setUser(null);
        router.replace("/login");
        return;
      }

      if (requiredRoles && !requiredRoles.includes(claims.role)) {
        router.replace(ROLE_HOME[claims.role]);
        return;
      }

      setChecking(false);
    } catch {
      setChecking(false);
      clearTokens();
      setUser(null);
      router.replace("/login");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isHydrated, isAuthenticated]);

  return { user, isLoading: checking || !isHydrated };
}
