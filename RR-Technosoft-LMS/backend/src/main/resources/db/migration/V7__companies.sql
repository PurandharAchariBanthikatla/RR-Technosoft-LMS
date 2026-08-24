-- =====================================================================
-- RR TECHNOSOFT LMS — Company Management (V7)
-- New `companies` directory backing the Placement module. Existing
-- `placements` table (V1) keeps its own denormalized company_name /
-- company_logo_url columns for backward compatibility with what's
-- already posted; V8 adds an optional company_id FK so new drives can
-- be linked to a managed company record.
-- =====================================================================

CREATE TABLE companies (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                 VARCHAR(200) NOT NULL,
    logo_url             TEXT,
    website              TEXT,
    industry             VARCHAR(150),
    description          TEXT,
    contact_person_name  VARCHAR(150),
    contact_email        VARCHAR(150),
    contact_phone        VARCHAR(30),
    address              VARCHAR(300),
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_by           UUID NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_companies_name_lower ON companies (lower(name));
CREATE INDEX idx_companies_is_active ON companies (is_active);
