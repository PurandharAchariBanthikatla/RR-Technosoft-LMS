-- Reports & Analytics module.
--
-- No new tables: every report in this module (student, faculty, attendance,
-- assignment, revenue, dashboard KPIs) is derived entirely from data already
-- captured by the Courses / Enrollments / Attendance / Assignments modules.
-- Revenue specifically is computed from enrollments.status + courses.price
-- (paid = ACTIVE or COMPLETED enrollments) since there is no separate
-- payments/invoices table in the schema yet — see ReportsService javadoc.
--
-- What this migration adds is purely the indexing needed for the aggregate
-- GROUP BY / date-range queries the reporting repositories run, so those
-- queries stay index-backed instead of falling back to sequential scans as
-- the tables grow.

-- Enrollments: revenue trend / student growth / faculty-completion queries
-- all filter or group by enrolled_at and status.
CREATE INDEX IF NOT EXISTS idx_enrollments_enrolled_at ON enrollments (enrolled_at);
CREATE INDEX IF NOT EXISTS idx_enrollments_status_enrolled_at ON enrollments (status, enrolled_at);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_status ON enrollments (course_id, status);

-- Attendance report groups by course and filters by date range.
CREATE INDEX IF NOT EXISTS idx_attendance_course_date ON attendance (course_id, attendance_date);
CREATE INDEX IF NOT EXISTS idx_attendance_student_date ON attendance (student_id, attendance_date);

-- Assignment report groups submissions by assignment and status, and filters
-- assignments by course + due date.
CREATE INDEX IF NOT EXISTS idx_assignments_course_due_at ON assignments (course_id, due_at);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_status ON assignment_submissions (assignment_id, status);
CREATE INDEX IF NOT EXISTS idx_submissions_student_status ON assignment_submissions (student_id, status);

-- Faculty report groups courses by instructor_name.
CREATE INDEX IF NOT EXISTS idx_courses_instructor_name ON courses (instructor_name);

-- Student report filters/searches users by role + full_name and joins
-- student_profiles on batch/branch/college.
CREATE INDEX IF NOT EXISTS idx_users_role_full_name ON users (role, full_name);
CREATE INDEX IF NOT EXISTS idx_student_profiles_batch ON student_profiles (batch);
CREATE INDEX IF NOT EXISTS idx_student_profiles_branch ON student_profiles (branch);

-- Dashboard "upcoming live classes" widget filters by status + scheduled_start.
CREATE INDEX IF NOT EXISTS idx_live_classes_status_start ON live_classes (status, scheduled_start);
