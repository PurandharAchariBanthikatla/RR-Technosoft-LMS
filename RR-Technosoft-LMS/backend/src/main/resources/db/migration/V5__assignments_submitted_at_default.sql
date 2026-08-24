-- =====================================================================
-- RR TECHNOSOFT LMS — Assignments module wiring (V5)
-- No structural changes needed: assignments / assignment_submissions
-- already match the JPA model this migration ships alongside. This
-- migration only adds the indexes the new query paths need.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_assignments_course ON assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment ON assignment_submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_student ON assignment_submissions(student_id);
