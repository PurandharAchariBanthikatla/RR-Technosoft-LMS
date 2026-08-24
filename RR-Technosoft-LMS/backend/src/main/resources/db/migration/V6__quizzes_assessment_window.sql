-- =====================================================================
-- RR TECHNOSOFT LMS — Assessment module (Online Exams / Quiz Attempts)
-- alignment (V6)
-- The frontend Quiz type (types/index.ts) needs an availability window
-- (availableFrom/availableTo) that quizzes never had — a quiz previously
-- had no concept of "open" vs "not yet open" vs "closed", only a
-- time-limit-per-attempt. Backfilled to a 30-day window from creation so
-- existing rows (if any) don't end up permanently unavailable.
-- One attempt per student per quiz is enforced (no retakes) to match the
-- pass/fail semantics implied by `pass_score_pct`.
-- =====================================================================

ALTER TABLE quizzes
    ADD COLUMN available_from TIMESTAMPTZ,
    ADD COLUMN available_to TIMESTAMPTZ;

UPDATE quizzes
SET available_from = created_at,
    available_to = created_at + INTERVAL '30 days'
WHERE available_from IS NULL;

ALTER TABLE quizzes
    ALTER COLUMN available_from SET NOT NULL,
    ALTER COLUMN available_to SET NOT NULL;

ALTER TABLE quiz_attempts
    ADD CONSTRAINT uq_quiz_attempts_quiz_student UNIQUE (quiz_id, student_id);

CREATE INDEX idx_quiz_questions_quiz ON quiz_questions(quiz_id);
CREATE INDEX idx_quiz_attempts_student ON quiz_attempts(student_id);
