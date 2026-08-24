-- =====================================================================
-- RR TECHNOSOFT LMS — Job Drives: extend `placements` (V8)
-- Adds the fields Job Drive management needs on top of the V1 table,
-- without touching any existing column. company_id is nullable so
-- pre-existing rows (and drives created without picking a managed
-- company) stay valid; company_name/company_logo_url remain the
-- source of truth for display and are kept in sync by the service
-- layer whenever company_id is set.
-- =====================================================================

ALTER TYPE placement_status ADD VALUE IF NOT EXISTS 'COMPLETED';
ALTER TYPE placement_status ADD VALUE IF NOT EXISTS 'CANCELLED';

CREATE TYPE job_type AS ENUM ('FULL_TIME', 'INTERNSHIP', 'PART_TIME', 'CONTRACT');

ALTER TABLE placements
    ADD COLUMN company_id       UUID REFERENCES companies(id) ON DELETE SET NULL,
    ADD COLUMN job_type         job_type NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN min_cgpa         NUMERIC(3,2),
    ADD COLUMN allowed_branches JSONB NOT NULL DEFAULT '[]',
    ADD COLUMN drive_date       DATE,
    ADD COLUMN updated_at       TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_placements_status ON placements (status);
CREATE INDEX idx_placements_company ON placements (company_id);
CREATE INDEX idx_placements_drive_date ON placements (drive_date);
