-- =====================================================================
-- RR TECHNOSOFT LMS — Learning Resources (V11)
-- Standalone resource library, optionally scoped to a course. Files
-- are stored in S3; file_url/file_key are populated by the upload
-- endpoint. A resource may instead just be an external link
-- (external_url), hence the OR check rather than requiring both.
-- =====================================================================

CREATE TYPE resource_type AS ENUM
    ('DOCUMENT', 'PDF', 'PRESENTATION', 'SPREADSHEET', 'LINK', 'ARCHIVE', 'OTHER');

CREATE TABLE learning_resources (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    resource_type    resource_type NOT NULL DEFAULT 'DOCUMENT',
    category         VARCHAR(100),
    course_id        UUID REFERENCES courses(id) ON DELETE SET NULL,
    file_url         TEXT,
    file_key         TEXT,
    file_size_bytes  BIGINT,
    external_url     TEXT,
    is_published     BOOLEAN NOT NULL DEFAULT true,
    download_count   BIGINT NOT NULL DEFAULT 0,
    uploaded_by      UUID NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_learning_resource_has_source CHECK (file_url IS NOT NULL OR external_url IS NOT NULL)
);

CREATE INDEX idx_learning_resources_course ON learning_resources (course_id);
CREATE INDEX idx_learning_resources_category ON learning_resources (category);
CREATE INDEX idx_learning_resources_published ON learning_resources (is_published);
