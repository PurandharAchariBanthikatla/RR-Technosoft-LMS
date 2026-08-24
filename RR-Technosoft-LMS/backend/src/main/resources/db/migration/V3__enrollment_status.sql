-- =====================================================================
-- RR TECHNOSOFT LMS — Enrollment status (V3)
-- Brings `enrollments` in line with the EnrollmentStatus contract the
-- frontend (types/index.ts, admin/enrollments page) already expects:
-- ACTIVE / COMPLETED / DROPPED / PENDING. The table previously only
-- inferred completion from `completed_at IS NOT NULL`, with no way to
-- represent PENDING (invited, not yet started) or DROPPED.
-- =====================================================================

CREATE TYPE enrollment_status AS ENUM ('ACTIVE', 'COMPLETED', 'DROPPED', 'PENDING');

ALTER TABLE enrollments
    ADD COLUMN status enrollment_status NOT NULL DEFAULT 'ACTIVE';

-- Backfill from the existing completed_at signal.
UPDATE enrollments SET status = 'COMPLETED' WHERE completed_at IS NOT NULL;

CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
