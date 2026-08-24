-- =====================================================================
-- RR TECHNOSOFT LMS — Student Applications tracking (V9)
-- V1's `placement_applications` only recorded that a student applied
-- (placement_id, student_id, applied_at). This adds the status
-- lifecycle, resume attachment and admin notes needed to actually
-- track an application through to an outcome.
-- =====================================================================

CREATE TYPE application_status AS ENUM
    ('APPLIED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'SELECTED', 'REJECTED', 'WITHDRAWN');

ALTER TABLE placement_applications
    ADD COLUMN status      application_status NOT NULL DEFAULT 'APPLIED',
    ADD COLUMN resume_url  TEXT,
    ADD COLUMN notes       TEXT,
    ADD COLUMN updated_at  TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_placement_applications_student ON placement_applications (student_id);
CREATE INDEX idx_placement_applications_placement ON placement_applications (placement_id);
CREATE INDEX idx_placement_applications_status ON placement_applications (status);
