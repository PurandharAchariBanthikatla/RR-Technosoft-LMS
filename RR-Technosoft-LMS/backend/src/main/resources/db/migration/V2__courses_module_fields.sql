-- =====================================================================
-- RR TECHNOSOFT LMS — Courses module field alignment (V2)
-- Brings `courses` in line with the catalog contract the frontend
-- (Course type / CourseForm) already expects: draft/publish workflow,
-- pricing, weekly duration, and a display instructor name that doesn't
-- require the Faculty module (not built yet) to exist first.
-- =====================================================================

CREATE TYPE course_status AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

ALTER TABLE courses
    ADD COLUMN status course_status NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN price NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN duration_weeks INT,
    ADD COLUMN instructor_name VARCHAR(150),
    ADD COLUMN rating NUMERIC(2,1);

-- Backfill status from the old boolean, then drop it.
UPDATE courses
SET status =
CASE
WHEN is_published THEN 'PUBLISHED'::course_status
ELSE 'DRAFT'::course_status
END;
ALTER TABLE courses DROP COLUMN is_published;

-- duration_hours was never populated by any shipped code path; duration_weeks
-- is the unit the catalog UI and CourseForm use. Drop the unused column
-- rather than keep two duration fields to reconcile.
ALTER TABLE courses DROP COLUMN duration_hours;

CREATE INDEX idx_courses_status ON courses(status);

COMMENT ON COLUMN courses.instructor_id IS
    'Nullable FK reserved for the Faculty module. Until Faculty ships, '
    'instructor_name (free text, set via CourseForm) is the source of truth '
    'for display.';
COMMENT ON COLUMN courses.rating IS
    'Not yet writable via any API — reserved for a future course-reviews module. '
    'Always null until then.';
