# RR TECHNOSOFT LMS — Backend (Phase 1: Foundation & Auth)

Spring Boot 3.3 / Java 21 / PostgreSQL 15 / Flyway / JWT.

## What's in this phase

- **Full database schema** (`src/main/resources/db/migration/V1__init_schema.sql`)
  covering every feature in the spec: users/roles, courses/modules/lessons,
  enrollments & progress, assignments, daily tasks, quizzes, attendance,
  live classes, certificates, placements, practice/coding portal
  (problems, submissions, badges, leaderboard), announcements,
  notifications, AI chatbot conversation history, and audit logs.
- **Authentication & RBAC**
  - One `/auth/login` endpoint for everyone. Admins send their email;
    students send their Student ID (format `RRT2026S0001`, auto-generated).
    The service tells them apart by whether the identifier contains `@`.
  - Stateless JWT access tokens (15 min default) + rotating opaque refresh
    tokens (7 days default), refresh tokens stored only as SHA-256 hashes.
  - Account lockout after 5 failed attempts (15 min, both configurable).
  - Three roles: `SUPER_ADMIN` (exactly one, enforced by a DB partial
    unique index), `ADMIN` (capped at 10, enforced in
    `AdminManagementService`), `STUDENT`.
  - `SecurityConfig` maps URL prefixes to roles; `@PreAuthorize` on
    controllers is a second layer of defense.
- **Super Admin can**: create/activate/suspend/delete Admin accounts
  (`/admins/**`).
- **Admin/Super Admin can**: create/activate/suspend/delete Student
  accounts (`/students/manage/**`).
- **Audit logging**: every sensitive action (login, admin/student
  create/delete/status change) writes to `audit_logs`.

## Not yet built (next phases)

Course/content management APIs, enrollments & progress tracking,
assignments/quizzes/daily tasks endpoints, live classes & attendance,
certificates, placements, practice portal & leaderboard, notifications,
AI chatbot endpoint, S3 upload service, and the Next.js frontend. The
schema already models all of it — the next phase wires REST controllers
and services on top of these tables, following the exact same pattern as
`AdminManagementService`/`StudentManagementService` in this phase.

## Running locally

Requires Java 21, Maven, PostgreSQL 15+ (and Redis, optional for phase 1).

```bash
createdb rr_lms
export DB_URL=jdbc:postgresql://localhost:5432/rr_lms
export DB_USERNAME=lms_user
export DB_PASSWORD=your_password
export JWT_SECRET=$(openssl rand -base64 48)
export SUPER_ADMIN_EMAIL=superadmin@rrtechnosoft.com
export SUPER_ADMIN_PASSWORD='ChangeMe@123!'

mvn spring-boot:run
```

Flyway applies `V1__init_schema.sql` automatically on startup, and
`DataSeeder` creates the Super Admin account if none exists yet — log the
console output for confirmation, then log in and change the password.

Swagger UI: `http://localhost:8080/api/v1/docs/swagger-ui.html`

## API surface so far

| Method | Path | Access |
|---|---|---|
| POST | `/auth/login` | Public — `{identifier, password}` |
| POST | `/auth/refresh` | Public — `{refreshToken}` |
| POST | `/auth/logout` | Authenticated |
| POST | `/admins` | Super Admin |
| GET | `/admins` | Super Admin |
| PATCH | `/admins/{id}/status?status=SUSPENDED` | Super Admin |
| DELETE | `/admins/{id}` | Super Admin |
| POST | `/students/manage` | Admin, Super Admin |
| GET | `/students/manage?search=` | Admin, Super Admin |
| PATCH | `/students/manage/{id}/status?status=SUSPENDED` | Admin, Super Admin |
| DELETE | `/students/manage/{id}` | Admin, Super Admin |

## Environment variables (production)

See `application.yml` — every value has an env var override
(`JWT_SECRET`, `DB_URL`, `S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `SMTP_HOST`,
etc.). Never commit real secrets; this repo ships only placeholders.
