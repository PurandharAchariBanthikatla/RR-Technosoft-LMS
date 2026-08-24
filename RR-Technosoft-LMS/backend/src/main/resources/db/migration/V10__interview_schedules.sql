-- =====================================================================
-- RR TECHNOSOFT LMS — Interview Tracking (V10)
-- One placement_application can go through several interview rounds;
-- each round is its own row here so status/result/feedback can be
-- tracked and updated independently per round.
-- =====================================================================

CREATE TYPE interview_mode AS ENUM ('ONLINE', 'OFFLINE', 'TELEPHONIC');
CREATE TYPE interview_status AS ENUM ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED');
CREATE TYPE interview_result AS ENUM ('PENDING', 'PASS', 'FAIL', 'ON_HOLD');

CREATE TABLE interview_schedules (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id    UUID NOT NULL REFERENCES placement_applications(id) ON DELETE CASCADE,
    round_number      INT NOT NULL DEFAULT 1,
    round_name        VARCHAR(100) NOT NULL,
    scheduled_at      TIMESTAMPTZ NOT NULL,
    mode              interview_mode NOT NULL DEFAULT 'ONLINE',
    venue_or_link     TEXT,
    interviewer_name  VARCHAR(150),
    status            interview_status NOT NULL DEFAULT 'SCHEDULED',
    result            interview_result NOT NULL DEFAULT 'PENDING',
    feedback          TEXT,
    created_by        UUID NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_schedules_application ON interview_schedules (application_id);
CREATE INDEX idx_interview_schedules_scheduled_at ON interview_schedules (scheduled_at);
