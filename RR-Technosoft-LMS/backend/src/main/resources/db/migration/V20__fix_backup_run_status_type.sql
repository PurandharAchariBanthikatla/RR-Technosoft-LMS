-- Align backup_runs.status with Hibernate @Enumerated(EnumType.STRING).

ALTER TABLE backup_runs
    ALTER COLUMN status TYPE VARCHAR(20)
    USING status::text;
