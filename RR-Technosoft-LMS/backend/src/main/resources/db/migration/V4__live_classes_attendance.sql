-- =====================================================================
-- RR TECHNOSOFT LMS — Live Classes + Attendance alignment (V4)
-- The frontend (lib/api/attendance.ts, AttendanceRecord type) marks and
-- reads attendance by (courseId, date, studentId) directly — not by a
-- specific live_class_id — so a session can be marked present/absent
-- for a course/day even when it isn't tied to one particular scheduled
-- live class row. live_class_id is kept as an optional link back to the
-- session that produced the record; course_id + attendance_date become
-- the primary, always-populated identity of a record.
-- =====================================================================

ALTER TABLE attendance
    ALTER COLUMN live_class_id DROP NOT NULL,
    ADD COLUMN course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
    ADD COLUMN attendance_date DATE;

-- Backfill from the live class each existing row points at.
UPDATE attendance a
SET course_id = lc.course_id,
    attendance_date = lc.scheduled_start::date
FROM live_classes lc
WHERE a.live_class_id = lc.id;

ALTER TABLE attendance
    ALTER COLUMN course_id SET NOT NULL,
    ALTER COLUMN attendance_date SET NOT NULL;

ALTER TABLE attendance DROP CONSTRAINT attendance_live_class_id_student_id_key;
ALTER TABLE attendance ADD CONSTRAINT uq_attendance_course_student_date
    UNIQUE (course_id, student_id, attendance_date);

CREATE INDEX idx_attendance_course_date ON attendance(course_id, attendance_date);
CREATE INDEX idx_attendance_student ON attendance(student_id);

COMMENT ON COLUMN attendance.live_class_id IS
    'Optional link to the live-class session the record was taken in. '
    'Nullable because attendance can be marked directly for a course/date '
    '(see AttendanceController#mark) without one.';
