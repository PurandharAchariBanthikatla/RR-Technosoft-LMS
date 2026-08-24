# RR TECHNOSOFT — Learning Management System (Frontend)

A production-ready **Next.js 15 + TypeScript + Tailwind CSS + shadcn/ui** frontend for the RR
TECHNOSOFT LMS, covering both the **Admin Portal** and the **Student Portal**. Built to talk to a
Java Spring Boot REST backend over JWT-authenticated APIs.

## Stack

- **Next.js 15** (App Router, Server + Client Components)
- **TypeScript** (strict mode)
- **Tailwind CSS** with a custom RR TECHNOSOFT design token system (red / black / white)
- **shadcn/ui**-style components (hand-rolled on Radix primitives, no CLI lock-in)
- **Zustand** for auth/UI state, with `persist` for session rehydration
- **React Hook Form + Zod** for forms and validation
- **Axios** with request/response interceptors for JWT attach + silent refresh
- **next-themes** for light/dark mode
- **Recharts** for dashboard analytics
- **Sonner** for toast notifications

## Getting started

```bash
npm install
cp .env.example .env.local   # point NEXT_PUBLIC_API_BASE_URL at your Spring Boot API
npm run dev
```

The app expects a Spring Boot backend exposing the routes listed in
`src/lib/constants.ts` (`API_ROUTES`). Update that file if your backend uses different paths.

> This project was generated in a sandboxed environment without network access, so
> dependencies have **not** been installed or built here — run `npm install` locally to pull
> them down and verify the build.

## Folder structure

```
src/
  app/
    (auth)/            # login, register, forgot-password — split brand/form layout
    (admin)/admin/      # Admin Portal — courses, students, enrollments, assignments,
                        # quizzes, live classes, attendance, certificates, placements
    (student)/student/  # Student Portal — dashboard, courses, daily tasks, assignments,
                        # quizzes, live classes, attendance, practice portal,
                        # certificates, placements
    profile/            # shared profile settings (both roles)
  components/
    ui/                 # shadcn-style primitives (button, card, dialog, table, ...)
    layout/             # Sidebar, Topbar, ThemeToggle/Provider
    shared/             # PageHeader, StatCard, DataTable, EmptyState, ErrorState, ...
    auth/                # LoginForm, RegisterForm
    courses/             # CourseCard, CourseForm
    dashboard/           # ProgressRing
  lib/
    api/                 # one service module per domain, all built on a shared axios client
    validations/         # zod schemas
    constants.ts          # roles, nav homes, API_ROUTES
    nav-config.ts          # sidebar nav items per portal
    utils.ts
  store/                  # zustand: auth-store (JWT + user), ui-store (sidebar/mobile nav)
  hooks/                  # useAuth (route guard), useFetch (data fetching), useDebounce
  types/                  # shared TS types for every domain entity
middleware.ts             # coarse role-based route protection via a lightweight cookie
```

## Backend alignment (phase 1)

This frontend is now wired to match the real `rr-technosoft-lms-backend` (Spring Boot) contract
for its **phase 1** scope — auth + admin/student user management:

- **Login** sends `{ identifier, password }` (`identifier` = email for `SUPER_ADMIN`/`ADMIN`, or a
  generated Student ID like `RRT2026S0001` for `STUDENT`) to `POST /auth/login`, matching
  `AuthController` / `LoginRequest` exactly.
- **No self-registration.** There's no public sign-up — `SUPER_ADMIN`s create admins via
  `/admin/admins` (`POST /admins`) and admins create students via `/admin/students`
  (`POST /students/manage`). See `lib/api/admins.ts` and `lib/api/students.ts`.
- **No `/auth/me`.** The access token's JWT claims (`role`, `email`, `studentId`, `fullName`, `sub`,
  `exp`) are decoded client-side in `hooks/use-auth.ts` (via `jwt-decode`) to verify the session and
  enforce role-based routing on every protected page — there's no extra round trip.
- **Roles** are `SUPER_ADMIN`, `ADMIN`, `STUDENT` (matching `UserRole.java` exactly); an
  `Admins` nav item only appears in the sidebar for `SUPER_ADMIN`, mirroring the backend's
  `@PreAuthorize("hasRole('SUPER_ADMIN')")` lock on `/admins/**`.
- **Pagination** types (`Paginated<T>`) match Spring Data's default `Page<T>` JSON shape
  (`content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`, `empty`) — note it's
  `number`, not `page`.
- Everything else — courses, modules, quizzes, live classes, attendance, certificates, placements,
  practice portal — is **not implemented on the backend yet**. Those pages/services describe the
  contract this frontend expects for later phases; they'll return 404s against the phase-1 backend
  until the matching Spring controllers ship. Update `lib/constants.ts` → `API_ROUTES` and the
  corresponding `lib/api/*.ts` file if a later phase's real contract differs from what's stubbed here.

## Authentication & role-based routing

- On login/register, the backend's JWT access + refresh tokens are stored in `localStorage`,
  and a lightweight `rr_role` cookie is set so `middleware.ts` can do a fast, edge-level
  redirect for the wrong portal (e.g. a student hitting `/admin/*`).
- Every protected page is also guarded client-side via the `useAuth()` hook, which re-verifies
  the session against `GET /auth/me` and redirects on failure — defense in depth beyond the
  cookie check.
- `lib/api/client.ts` attaches the JWT to every request and transparently refreshes it on a
  401, queuing concurrent requests while the refresh is in flight.

## Design system

- **Brand colors**: RR TECHNOSOFT red (`#E31E24`) as the primary/action color, a near-black
  ink (`#0B0B0C`) for sidebars and dark surfaces, white/neutral surfaces for content — with a
  full dark mode palette derived from the same tokens (see `src/app/globals.css`).
- **Typography**: Space Grotesk for display/headings, Inter for body text, JetBrains Mono for
  the practice portal's code editor.
- Fully responsive: collapsible/off-canvas sidebar on mobile, responsive grids throughout.

## Extending the app

Each feature area (courses, assignments, quizzes, attendance, etc.) follows the same pattern:

1. A typed API service in `src/lib/api/*.ts`
2. A page in `src/app/(admin)/admin/*` and/or `src/app/(student)/student/*` that calls it via
   `useFetch()` and renders `DataTable` / cards with consistent loading, error and empty states
3. Shared UI building blocks (`PageHeader`, `StatusBadge`, `ConfirmDialog`, ...) for consistency

To add a new resource, copy this pattern rather than one-off components.
