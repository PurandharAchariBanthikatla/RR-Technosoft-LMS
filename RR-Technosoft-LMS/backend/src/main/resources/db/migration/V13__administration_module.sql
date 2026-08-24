-- =====================================================================
-- RR TECHNOSOFT LMS — Administration Module (V13)
-- Settings, Master Data, Permission Matrix, System Configuration
-- PostgreSQL 15+
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM TYPES
-- ---------------------------------------------------------------------
CREATE TYPE setting_value_type AS ENUM ('STRING', 'NUMBER', 'BOOLEAN', 'JSON');
CREATE TYPE setting_category AS ENUM ('GENERAL', 'ACADEMICS', 'ENGAGEMENT', 'SECURITY', 'INTEGRATIONS');
CREATE TYPE backup_storage_type AS ENUM ('LOCAL', 'S3');
CREATE TYPE backup_run_status AS ENUM ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED');
CREATE TYPE digest_frequency AS ENUM ('INSTANT', 'DAILY', 'WEEKLY', 'NONE');

-- ---------------------------------------------------------------------
-- PERMISSIONS + ROLE PERMISSION MATRIX
-- Permissions are seeded once as a catalogue; which roles hold which
-- permission is fully data-driven via role_permissions so a Super Admin
-- can regrant/revoke access from the UI with zero code changes.
-- ---------------------------------------------------------------------
CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    category        VARCHAR(60) NOT NULL DEFAULT 'GENERAL',
    is_system       BOOLEAN NOT NULL DEFAULT true,  -- system permissions can't be deleted, only regranted
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_permissions_category ON permissions(category);

CREATE TABLE role_permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role            user_role NOT NULL,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    allowed         BOOLEAN NOT NULL DEFAULT false,
    updated_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_permission UNIQUE (role, permission_id)
);
CREATE INDEX idx_role_permissions_role ON role_permissions(role);

-- ---------------------------------------------------------------------
-- SYSTEM SETTINGS — generic typed key/value store for platform-wide
-- preferences that don't warrant their own table (session banners,
-- default page size, maintenance mode, etc.)
-- ---------------------------------------------------------------------
CREATE TABLE system_settings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key     VARCHAR(150) NOT NULL UNIQUE,
    setting_value   TEXT,
    value_type      setting_value_type NOT NULL DEFAULT 'STRING',
    category        setting_category NOT NULL DEFAULT 'GENERAL',
    description     TEXT,
    is_editable     BOOLEAN NOT NULL DEFAULT true,
    updated_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_system_settings_category ON system_settings(category);

-- ---------------------------------------------------------------------
-- ORGANIZATION PROFILE — singleton row (branding, contact, locale)
-- ---------------------------------------------------------------------
CREATE TABLE organization_profile (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    singleton_guard     BOOLEAN NOT NULL DEFAULT true,
    org_name            VARCHAR(200) NOT NULL DEFAULT 'RR TECHNOSOFT',
    legal_name          VARCHAR(200),
    logo_url            TEXT,
    favicon_url         TEXT,
    website             VARCHAR(255),
    support_email       VARCHAR(255),
    support_phone       VARCHAR(20),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100) DEFAULT 'India',
    postal_code         VARCHAR(20),
    tax_id              VARCHAR(50),
    timezone            VARCHAR(60) NOT NULL DEFAULT 'Asia/Kolkata',
    date_format         VARCHAR(20) NOT NULL DEFAULT 'dd-MM-yyyy',
    updated_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_org_profile_singleton UNIQUE (singleton_guard)
);

-- ---------------------------------------------------------------------
-- MASTER DATA — generic category/item CRUD so Super Admin can maintain
-- lookup lists (departments, designations, skill tags, course
-- categories, document types...) without redeploying code.
-- ---------------------------------------------------------------------
CREATE TABLE master_data_categories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(80) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT false,  -- system categories can't be deleted
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE master_data_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id     UUID NOT NULL REFERENCES master_data_categories(id) ON DELETE CASCADE,
    code            VARCHAR(100) NOT NULL,
    label           VARCHAR(200) NOT NULL,
    description     TEXT,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_master_data_item UNIQUE (category_id, code)
);
CREATE INDEX idx_master_data_items_category ON master_data_items(category_id);
CREATE INDEX idx_master_data_items_active ON master_data_items(is_active);

-- ---------------------------------------------------------------------
-- FEATURE TOGGLES
-- ---------------------------------------------------------------------
CREATE TABLE feature_toggles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_key     VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    updated_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- NOTIFICATION / EMAIL SETTINGS — singleton row
-- ---------------------------------------------------------------------
CREATE TABLE notification_settings (
    id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    singleton_guard                 BOOLEAN NOT NULL DEFAULT true,
    smtp_host                       VARCHAR(255),
    smtp_port                       INT NOT NULL DEFAULT 587,
    smtp_username                   VARCHAR(255),
    smtp_password_encrypted         TEXT,
    smtp_use_tls                    BOOLEAN NOT NULL DEFAULT true,
    from_name                       VARCHAR(150) NOT NULL DEFAULT 'RR TECHNOSOFT LMS',
    from_email                      VARCHAR(255),
    email_notifications_enabled     BOOLEAN NOT NULL DEFAULT true,
    sms_notifications_enabled       BOOLEAN NOT NULL DEFAULT false,
    push_notifications_enabled      BOOLEAN NOT NULL DEFAULT false,
    digest_frequency                digest_frequency NOT NULL DEFAULT 'DAILY',
    updated_by                      UUID REFERENCES users(id),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_settings_singleton UNIQUE (singleton_guard)
);

-- ---------------------------------------------------------------------
-- SECURITY SETTINGS — singleton row
-- ---------------------------------------------------------------------
CREATE TABLE security_settings (
    id                                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    singleton_guard                     BOOLEAN NOT NULL DEFAULT true,
    password_min_length                 INT NOT NULL DEFAULT 8,
    password_require_uppercase          BOOLEAN NOT NULL DEFAULT true,
    password_require_number             BOOLEAN NOT NULL DEFAULT true,
    password_require_special_char       BOOLEAN NOT NULL DEFAULT true,
    password_expiry_days                INT NOT NULL DEFAULT 90,
    max_login_attempts                  INT NOT NULL DEFAULT 5,
    lockout_duration_minutes            INT NOT NULL DEFAULT 15,
    session_timeout_minutes             INT NOT NULL DEFAULT 60,
    mfa_required_for_admins             BOOLEAN NOT NULL DEFAULT false,
    allowed_ip_ranges                   TEXT,
    force_logout_on_password_change     BOOLEAN NOT NULL DEFAULT true,
    updated_by                          UUID REFERENCES users(id),
    created_at                          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_security_settings_singleton UNIQUE (singleton_guard)
);

-- ---------------------------------------------------------------------
-- BACKUP & RESTORE CONFIGURATION + RUN HISTORY
-- ---------------------------------------------------------------------
CREATE TABLE backup_configs (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    singleton_guard         BOOLEAN NOT NULL DEFAULT true,
    schedule_cron           VARCHAR(100) NOT NULL DEFAULT '0 0 2 * * *',
    retention_days          INT NOT NULL DEFAULT 30,
    storage_type            backup_storage_type NOT NULL DEFAULT 'LOCAL',
    storage_location        VARCHAR(500) NOT NULL DEFAULT '/var/backups/rr-lms',
    auto_backup_enabled     BOOLEAN NOT NULL DEFAULT true,
    updated_by              UUID REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_backup_config_singleton UNIQUE (singleton_guard)
);

CREATE TABLE backup_runs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backup_config_id    UUID NOT NULL REFERENCES backup_configs(id) ON DELETE CASCADE,
    status              backup_run_status NOT NULL DEFAULT 'PENDING',
    triggered_by        UUID REFERENCES users(id),   -- null = scheduled run
    file_location       TEXT,
    size_mb             NUMERIC(12, 2),
    error_message       TEXT,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ
);
CREATE INDEX idx_backup_runs_config ON backup_runs(backup_config_id);
CREATE INDEX idx_backup_runs_started_at ON backup_runs(started_at DESC);

-- ---------------------------------------------------------------------
-- SEED DATA — permission catalogue
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, name, description, category) VALUES
    ('COURSE_MANAGE',              'Manage Courses',              'Create, edit, publish and delete courses, modules and lessons', 'ACADEMICS'),
    ('ENROLLMENT_MANAGE',          'Manage Enrollments',          'Enroll or unenroll students from courses', 'ACADEMICS'),
    ('ASSIGNMENT_MANAGE',          'Manage Assignments',          'Create assignments and grade submissions', 'ACADEMICS'),
    ('QUIZ_MANAGE',                'Manage Quizzes',              'Create quizzes and review attempts', 'ACADEMICS'),
    ('LIVE_CLASS_MANAGE',          'Manage Live Classes',         'Schedule and manage live class sessions', 'ACADEMICS'),
    ('ATTENDANCE_MANAGE',          'Manage Attendance',           'Mark and correct attendance records', 'ACADEMICS'),
    ('CERTIFICATE_ISSUE',          'Issue Certificates',          'Issue and revoke student certificates', 'ACADEMICS'),
    ('PLACEMENT_MANAGE',           'Manage Placements',           'Create and manage placement drives', 'ENGAGEMENT'),
    ('ANNOUNCEMENT_MANAGE',        'Manage Announcements',        'Publish platform-wide announcements', 'ENGAGEMENT'),
    ('STUDENT_MANAGE',             'Manage Students',             'Create, edit, suspend and delete student accounts', 'ADMINISTRATION'),
    ('ADMIN_MANAGE',               'Manage Admin Accounts',       'Create, edit and remove Admin accounts', 'ADMINISTRATION'),
    ('PERMISSION_MATRIX_MANAGE',   'Manage Permission Matrix',    'Grant or revoke role-based permissions', 'ADMINISTRATION'),
    ('SETTINGS_MANAGE',            'Manage System Settings',      'Edit platform-wide system preferences', 'ADMINISTRATION'),
    ('MASTER_DATA_MANAGE',         'Manage Master Data',          'Maintain lookup lists such as departments and designations', 'ADMINISTRATION'),
    ('ORG_PROFILE_MANAGE',         'Manage Organization Profile', 'Edit organization branding and contact details', 'ADMINISTRATION'),
    ('FEATURE_TOGGLE_MANAGE',      'Manage Feature Toggles',      'Enable or disable platform features', 'ADMINISTRATION'),
    ('SECURITY_SETTINGS_MANAGE',   'Manage Security Settings',    'Edit password policy, lockout and session rules', 'SECURITY'),
    ('NOTIFICATION_SETTINGS_MANAGE','Manage Notification Settings','Configure SMTP and notification channel preferences', 'INTEGRATIONS'),
    ('BACKUP_MANAGE',              'Manage Backup & Restore',     'Configure and trigger database backups', 'ADMINISTRATION'),
    ('AUDIT_LOG_VIEW',             'View Audit Logs',             'View the platform audit trail', 'ADMINISTRATION');

-- SUPER_ADMIN gets every permission allowed.
INSERT INTO role_permissions (role, permission_id, allowed)
SELECT 'SUPER_ADMIN', id, true FROM permissions;

-- ADMIN gets the day-to-day academic/engagement permissions by default;
-- administration-tier permissions stay reserved for SUPER_ADMIN until a
-- Super Admin explicitly regrants them from the Permission Matrix UI.
INSERT INTO role_permissions (role, permission_id, allowed)
SELECT 'ADMIN', id,
    CASE WHEN category IN ('ACADEMICS', 'ENGAGEMENT') THEN true ELSE false END
FROM permissions;

-- STUDENT holds no administrative permissions by default.
INSERT INTO role_permissions (role, permission_id, allowed)
SELECT 'STUDENT', id, false FROM permissions;

-- ---------------------------------------------------------------------
-- SEED DATA — singleton configuration rows
-- ---------------------------------------------------------------------
INSERT INTO organization_profile (org_name, support_email, timezone) VALUES
    ('RR TECHNOSOFT', 'support@rrtechnosoft.com', 'Asia/Kolkata');

INSERT INTO notification_settings (from_name, from_email) VALUES
    ('RR TECHNOSOFT LMS', 'no-reply@rrtechnosoft.com');

INSERT INTO security_settings DEFAULT VALUES;

INSERT INTO backup_configs (storage_type, storage_location) VALUES
    ('LOCAL', '/var/backups/rr-lms');

-- ---------------------------------------------------------------------
-- SEED DATA — default feature toggles
-- ---------------------------------------------------------------------
INSERT INTO feature_toggles (feature_key, name, description, enabled) VALUES
    ('AI_CHATBOT',          'AI Chatbot',            'Student-facing 24/7 AI chatbot for technical Q&A and interview prep', true),
    ('PRACTICE_PORTAL',     'Practice Portal',       'KodNest-style coding practice section with leaderboards and badges', true),
    ('PLACEMENTS',          'Placements Module',     'Placement drives and application tracking', true),
    ('LIVE_CLASSES',        'Live Classes',          'Google Meet / Zoom / Teams live class scheduling', true),
    ('CERTIFICATES',        'Certificates',          'Automatic and manual certificate issuance', true),
    ('MAINTENANCE_MODE',    'Maintenance Mode',      'Show a maintenance banner and block student logins', false);

-- ---------------------------------------------------------------------
-- SEED DATA — default system settings
-- ---------------------------------------------------------------------
INSERT INTO system_settings (setting_key, setting_value, value_type, category, description) VALUES
    ('platform.default_page_size',     '20',       'NUMBER',  'GENERAL',  'Default page size for admin list views'),
    ('platform.maintenance_message',   'RR TECHNOSOFT LMS is undergoing scheduled maintenance. Please check back soon.', 'STRING', 'GENERAL', 'Message shown when maintenance mode is enabled'),
    ('academics.certificate_prefix',   'RRT-CERT-', 'STRING', 'ACADEMICS', 'Prefix used when generating certificate numbers'),
    ('academics.passing_percentage',   '60',       'NUMBER',  'ACADEMICS', 'Default passing percentage applied to new quizzes'),
    ('engagement.leaderboard_size',    '50',       'NUMBER',  'ENGAGEMENT', 'Number of students shown on the practice leaderboard');

-- ---------------------------------------------------------------------
-- MASTER DATA — default system categories + starter items
-- ---------------------------------------------------------------------
INSERT INTO master_data_categories (code, name, description, is_system) VALUES
    ('DEPARTMENT',       'Departments',        'Admin departments used on admin profiles', true),
    ('DESIGNATION',      'Designations',       'Admin job titles used on admin profiles', true),
    ('SKILL_TAG',        'Skill Tags',         'Tags used to classify course and student skills', true),
    ('COURSE_CATEGORY',  'Course Categories',  'Top-level categories used to group courses', true),
    ('DOCUMENT_TYPE',    'Document Types',     'Document types accepted for placement applications', true);

INSERT INTO master_data_items (category_id, code, label, sort_order)
SELECT id, v.code, v.label, v.sort_order FROM master_data_categories,
    (VALUES ('OPERATIONS', 'Operations', 1), ('ACADEMICS', 'Academics', 2), ('PLACEMENTS', 'Placements', 3), ('SUPPORT', 'Support', 4)) AS v(code, label, sort_order)
WHERE master_data_categories.code = 'DEPARTMENT';

INSERT INTO master_data_items (category_id, code, label, sort_order)
SELECT id, v.code, v.label, v.sort_order FROM master_data_categories,
    (VALUES ('TRAINER', 'Trainer', 1), ('SENIOR_TRAINER', 'Senior Trainer', 2), ('PROGRAM_MANAGER', 'Program Manager', 3), ('PLACEMENT_OFFICER', 'Placement Officer', 4)) AS v(code, label, sort_order)
WHERE master_data_categories.code = 'DESIGNATION';

INSERT INTO master_data_items (category_id, code, label, sort_order)
SELECT id, v.code, v.label, v.sort_order FROM master_data_categories,
    (VALUES ('AWS', 'AWS', 1), ('AZURE', 'Azure', 2), ('DEVOPS', 'DevOps', 3), ('DATA_ANALYTICS', 'Data Analytics', 4), ('LINUX', 'Linux', 5), ('DOCKER', 'Docker', 6), ('KUBERNETES', 'Kubernetes', 7), ('JENKINS', 'Jenkins', 8), ('TERRAFORM', 'Terraform', 9), ('GIT_GITHUB', 'Git & GitHub', 10), ('PYTHON', 'Python', 11), ('SQL', 'SQL', 12), ('POWER_BI', 'Power BI', 13), ('TABLEAU', 'Tableau', 14), ('JAVA', 'Java', 15), ('SPRING_BOOT', 'Spring Boot', 16)) AS v(code, label, sort_order)
WHERE master_data_categories.code = 'SKILL_TAG';

INSERT INTO master_data_items (category_id, code, label, sort_order)
SELECT id, v.code, v.label, v.sort_order FROM master_data_categories,
    (VALUES ('CLOUD_DEVOPS', 'Cloud & DevOps', 1), ('DATA', 'Data & Analytics', 2), ('PROGRAMMING', 'Programming', 3), ('BI_TOOLS', 'BI Tools', 4)) AS v(code, label, sort_order)
WHERE master_data_categories.code = 'COURSE_CATEGORY';

INSERT INTO master_data_items (category_id, code, label, sort_order)
SELECT id, v.code, v.label, v.sort_order FROM master_data_categories,
    (VALUES ('RESUME', 'Resume', 1), ('ID_PROOF', 'ID Proof', 2), ('OFFER_LETTER', 'Offer Letter', 3)) AS v(code, label, sort_order)
WHERE master_data_categories.code = 'DOCUMENT_TYPE';
