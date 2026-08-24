-- =====================================================================
-- RR TECHNOSOFT LMS — Video Library (V12)
-- Videos are either uploaded to S3 (source=UPLOAD, video_key set) or
-- linked externally (YOUTUBE/EXTERNAL, video_url only).
-- =====================================================================

CREATE TYPE video_source AS ENUM ('UPLOAD', 'YOUTUBE', 'EXTERNAL');

CREATE TABLE video_resources (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    category          VARCHAR(100),
    course_id         UUID REFERENCES courses(id) ON DELETE SET NULL,
    source            video_source NOT NULL DEFAULT 'UPLOAD',
    video_url         TEXT NOT NULL,
    video_key         TEXT,
    thumbnail_url     TEXT,
    duration_seconds  INT,
    file_size_bytes   BIGINT,
    is_published      BOOLEAN NOT NULL DEFAULT true,
    view_count        BIGINT NOT NULL DEFAULT 0,
    uploaded_by       UUID NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_video_resources_course ON video_resources (course_id);
CREATE INDEX idx_video_resources_category ON video_resources (category);
CREATE INDEX idx_video_resources_published ON video_resources (is_published);
