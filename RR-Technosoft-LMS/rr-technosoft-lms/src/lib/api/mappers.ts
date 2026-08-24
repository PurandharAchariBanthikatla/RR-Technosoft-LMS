import { AuthApiResponse, User, UserSummaryApiResponse } from "@/types";

export function mapAuthResponseToUser(res: AuthApiResponse): User {
  return {
    id: res.userId,
    name: res.fullName,
    email: res.email ?? undefined,
    studentId: res.studentId ?? undefined,
    role: res.role,
    createdAt: new Date().toISOString(),
  };
}

export function mapUserSummaryToUser(res: UserSummaryApiResponse): User {
  return {
    id: res.id,
    name: res.fullName,
    email: res.email ?? undefined,
    studentId: res.studentId ?? undefined,
    role: res.role,
    status: res.status,
    phone: res.phone ?? undefined,
    lastLoginAt: res.lastLoginAt ?? undefined,
    createdAt: res.createdAt,
  };
}
