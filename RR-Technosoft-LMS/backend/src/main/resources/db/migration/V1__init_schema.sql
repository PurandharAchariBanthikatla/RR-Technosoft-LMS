-- =====================================================================
-- RR TECHNOSOFT LMS — Core Schema (V1)
-- PostgreSQL 15+
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------
-- ENUM TYPES
-- ---------------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('SUPER_ADMIN', 'ADMIN', 'STUDENT');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING');
CREATE TYPE course_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED');
CREATE TYPE content_type AS ENUM ('VIDEO', 'PDF', 'ARTICLE', 'QUIZ', 'ASSIGNMENT', 'CODING_LAB', 'PROJECT');
CREATE TYPE submission_status AS ENUM ('NOT_SUBMITTED', 'SUBMITTED', 'LATE', 'GRADED', 'RESUBMIT_REQUESTED');
CREATE TYPE attendance_status AS ENUM ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED');
CREATE TYPE live_class_platform AS ENUM ('GOOGLE_MEET', 'ZOOM', 'MS_TEAMS');
CREATE TYPE live_class_status AS ENUM ('SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE difficulty_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED');
CREATE TYPE practice_track AS ENUM ('DSA', 'ALGORITHMS', 'SQL', 'PYTHON', 'JAVA', 'AWS', 'DEVOPS', 'DATA_ANALYTICS');
CREATE TYPE notification_type AS ENUM ('ANNOUNCEMENT', 'ASSIGNMENT', 'LIVE_CLASS', 'PLACEMENT', 'CERTIFICATE', 'SYSTEM', 'TASK');
CREATE TYPE placement_status AS ENUM ('OPEN', 'CLOSED', 'DRAFT');

-- ---------------------------------------------------------------------
-- USERS (single identity table for auth; role-specific detail in
-- admins / students). Admins auth with email, students with student_id.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role                user_role NOT NULL,
    email               VARCHAR(255) UNIQUE,           -- required for SUPER_ADMIN/ADMIN
    student_id          VARCHAR(50) UNIQUE,             -- required for STUDENT, e.g. RRT2026S0001
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150) NOT NULL,
    phone               VARCHAR(20),
    avatar_url           TEXT,
    status              account_status NOT NULL DEFAULT 'ACTIVE',
    failed_login_count  INT NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_admin_email CHECK (
        (role IN ('SUPER_ADMIN','ADMIN') AND email IS NOT NULL) OR role = 'STUDENT'
    ),
    CONSTRAINT chk_student_id CHECK (
        (role = 'STUDENT' AND student_id IS NOT NULL) OR role IN ('SUPER_ADMIN','ADMIN')
    )
);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- Enforce max 1 SUPER_ADMIN and max 10 ADMIN at the application layer
-- (DB-level partial unique index caps SUPER_ADMIN to 1 row).
CREATE UNIQUE INDEX uq_single_super_admin ON users ((role))
    WHERE role = 'SUPER_ADMIN';

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    user_agent  TEXT,
    ip_address  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------------
-- ADMIN / STUDENT PROFILE EXTENSIONS
-- ---------------------------------------------------------------------
CREATE TABLE admin_profiles (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    department      VARCHAR(100),
    designation     VARCHAR(100),
    permissions     JSONB NOT NULL DEFAULT '[]',   -- fine-grained scopes beyond base role
    assigned_by     UUID REFERENCES users(id)
);

CREATE TABLE student_profiles (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    batch               VARCHAR(50),
    branch              VARCHAR(100),
    college             VARCHAR(200),
    graduation_year     INT,
    resume_url          TEXT,
    linkedin_url        TEXT,
    github_url          TEXT,
    onboarded_at        TIMESTAMPTZ DEFAULT now()
);

-- ---------------------------------------------------------------------
-- COURSES / MODULES / LESSONS
-- ---------------------------------------------------------------------
CREATE TABLE courses (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) UNIQUE NOT NULL,
    category        VARCHAR(100) NOT NULL,     -- AWS, Azure, DevOps, Data Analytics, Linux, Docker, Kubernetes,
                                                -- Jenkins, Terraform, Git & GitHub, Python, SQL, Power BI, Tableau, Java, Spring Boot
    description     TEXT,
    thumbnail_url   TEXT,
    level           course_level NOT NULL DEFAULT 'BEGINNER',
    duration_hours  NUMERIC(6,1),
    instructor_id   UUID REFERENCES users(id),
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_courses_category ON courses(category);

CREATE TABLE course_modules (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    position    INT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (course_id, position)
);

CREATE TABLE lessons (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id       UUID NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    content_type    content_type NOT NULL,
    video_url       TEXT,                 -- S3 key/URL for recorded class video
    pdf_url         TEXT,                 -- S3 key/URL for notes
    body            TEXT,                 -- article / instructions markdown
    duration_minutes INT,
    position        INT NOT NULL,
    is_free_preview BOOLEAN NOT NULL DEFAULT FALSE,
    session_date    DATE,                 -- for "daily notes / daily recorded class"
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (module_id, position)
);
CREATE INDEX idx_lessons_session_date ON lessons(session_date);

CREATE TABLE lesson_resources (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id   UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    label       VARCHAR(200) NOT NULL,
    file_url    TEXT NOT NULL,
    file_type   VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- ENROLLMENTS / PROGRESS
-- ---------------------------------------------------------------------
CREATE TABLE enrollments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id       UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    enrolled_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    progress_pct    NUMERIC(5,2) NOT NULL DEFAULT 0,
    last_lesson_id  UUID REFERENCES lessons(id),   -- "continue where you left off"
    UNIQUE (student_id, course_id)
);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);

CREATE TABLE lesson_progress (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id   UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    lesson_id       UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    is_completed    BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INT NOT NULL DEFAULT 0,
    completed_at    TIMESTAMPTZ,
    UNIQUE (enrollment_id, lesson_id)
);

-- ---------------------------------------------------------------------
-- ASSIGNMENTS / QUIZZES / DAILY TASKS
-- ---------------------------------------------------------------------
CREATE TABLE assignments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id       UUID REFERENCES courses(id) ON DELETE CASCADE,
    module_id       UUID REFERENCES course_modules(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    instructions    TEXT,
    attachment_url  TEXT,
    max_score       INT NOT NULL DEFAULT 100,
    due_at          TIMESTAMPTZ,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assignment_submissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assignment_id   UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_url  TEXT,
    submission_text TEXT,
    status          submission_status NOT NULL DEFAULT 'NOT_SUBMITTED',
    score           INT,
    feedback        TEXT,
    submitted_at    TIMESTAMPTZ,
    graded_at       TIMESTAMPTZ,
    graded_by       UUID REFERENCES users(id),
    UNIQUE (assignment_id, student_id)
);

CREATE TABLE daily_tasks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id       UUID REFERENCES courses(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    task_date       DATE NOT NULL,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE daily_task_completions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id     UUID NOT NULL REFERENCES daily_tasks(id) ON DELETE CASCADE,
    student_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_done     BOOLEAN NOT NULL DEFAULT FALSE,
    done_at     TIMESTAMPTZ,
    UNIQUE (task_id, student_id)
);

CREATE TABLE quizzes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id   UUID REFERENCES course_modules(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    time_limit_minutes INT,
    pass_score_pct NUMERIC(5,2) NOT NULL DEFAULT 60,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE quiz_questions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id     UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question    TEXT NOT NULL,
    options     JSONB NOT NULL,        -- [{"key":"A","text":"..."}]
    correct_option VARCHAR(5) NOT NULL,
    position    INT NOT NULL
);

CREATE TABLE quiz_attempts (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id     UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    student_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    answers     JSONB NOT NULL DEFAULT '{}',
    score_pct   NUMERIC(5,2),
    started_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- ATTENDANCE / LIVE CLASSES
-- ---------------------------------------------------------------------
CREATE TABLE live_classes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id       UUID REFERENCES courses(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    platform        live_class_platform NOT NULL,
    meeting_link    TEXT NOT NULL,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end   TIMESTAMPTZ NOT NULL,
    status          live_class_status NOT NULL DEFAULT 'SCHEDULED',
    recording_url   TEXT,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_live_classes_start ON live_classes(scheduled_start);

CREATE TABLE attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    live_class_id   UUID NOT NULL REFERENCES live_classes(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          attendance_status NOT NULL DEFAULT 'ABSENT',
    marked_by       UUID REFERENCES users(id),
    marked_at       TIMESTAMPTZ DEFAULT now(),
    UNIQUE (live_class_id, student_id)
);

-- ---------------------------------------------------------------------
-- CERTIFICATES
-- ---------------------------------------------------------------------
CREATE TABLE certificates (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id       UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    certificate_no  VARCHAR(50) UNIQUE NOT NULL,
    file_url        TEXT,
    issued_by       UUID NOT NULL REFERENCES users(id),
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, course_id)
);

-- ---------------------------------------------------------------------
-- PLACEMENTS
-- ---------------------------------------------------------------------
CREATE TABLE placements (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_name        VARCHAR(200) NOT NULL,
    company_logo_url    TEXT,
    role_title           VARCHAR(200) NOT NULL,
    description         TEXT,
    eligibility         TEXT,
    skills_required     JSONB NOT NULL DEFAULT '[]',
    salary_min          NUMERIC(12,2),
    salary_max          NUMERIC(12,2),
    location            VARCHAR(150),
    last_date_to_apply  DATE NOT NULL,
    application_link    TEXT,
    status              placement_status NOT NULL DEFAULT 'OPEN',
    posted_by           UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE placement_applications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    placement_id    UUID NOT NULL REFERENCES placements(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (placement_id, student_id)
);

-- ---------------------------------------------------------------------
-- PRACTICE / CODING PORTAL
-- ---------------------------------------------------------------------
CREATE TABLE practice_problems (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(200) NOT NULL,
    track           practice_track NOT NULL,
    difficulty      difficulty_level NOT NULL,
    statement       TEXT NOT NULL,
    starter_code    TEXT,
    test_cases      JSONB NOT NULL DEFAULT '[]',
    points          INT NOT NULL DEFAULT 10,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_practice_problems_track ON practice_problems(track, difficulty);

CREATE TABLE practice_submissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    problem_id      UUID NOT NULL REFERENCES practice_problems(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code            TEXT NOT NULL,
    language        VARCHAR(30) NOT NULL,
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    runtime_ms      INT,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_practice_submissions_student ON practice_submissions(student_id);

CREATE TABLE badges (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url    TEXT,
    criteria    JSONB NOT NULL DEFAULT '{}'   -- e.g. {"problems_solved":50,"track":"SQL"}
);

CREATE TABLE student_badges (
    student_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id    UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    awarded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, badge_id)
);

-- Leaderboard is computed, but a materialized snapshot table keeps
-- reads O(1) instead of aggregating on every page load.
CREATE TABLE leaderboard_snapshot (
    student_id      UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_points    INT NOT NULL DEFAULT 0,
    problems_solved INT NOT NULL DEFAULT 0,
    rank            INT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- ANNOUNCEMENTS / NOTIFICATIONS
-- ---------------------------------------------------------------------
CREATE TABLE announcements (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    course_id   UUID REFERENCES courses(id) ON DELETE CASCADE,  -- NULL = global
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        notification_type NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT,
    link        TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read);

-- ---------------------------------------------------------------------
-- AI CHATBOT CONVERSATION HISTORY
-- ---------------------------------------------------------------------
CREATE TABLE chat_conversations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender          VARCHAR(10) NOT NULL CHECK (sender IN ('USER','BOT')),
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id, created_at);

-- ---------------------------------------------------------------------
-- AUDIT LOGS
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id    UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,      -- e.g. CREATE_ADMIN, DELETE_COURSE, ISSUE_CERTIFICATE
    entity_type VARCHAR(100),
    entity_id   UUID,
    metadata    JSONB DEFAULT '{}',
    ip_address  VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- ---------------------------------------------------------------------
-- updated_at trigger helper
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_courses_updated_at BEFORE UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
