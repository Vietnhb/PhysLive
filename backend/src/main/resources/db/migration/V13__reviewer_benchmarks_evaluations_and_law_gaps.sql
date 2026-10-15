-- Migration V13: Reviewer Benchmarks, Evaluation Runs, and Law Gaps
-- Hardening schema for Reviewer workflows

-- 1. Benchmark Problems
CREATE TABLE IF NOT EXISTS benchmark_problems (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    problem_text TEXT NOT NULL,
    topic VARCHAR(32) NOT NULL,
    grade_scope VARCHAR(32) NOT NULL,
    source_category VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_by_reference VARCHAR(80),
    archived_reason TEXT
);

ALTER TABLE IF EXISTS benchmark_problems
    ADD COLUMN IF NOT EXISTS status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by_reference VARCHAR(80),
    ADD COLUMN IF NOT EXISTS archived_reason TEXT;

ALTER TABLE IF EXISTS benchmark_problems
    DROP CONSTRAINT IF EXISTS ck_benchmark_problems_status;
ALTER TABLE IF EXISTS benchmark_problems
    ADD CONSTRAINT ck_benchmark_problems_status
    CHECK (status IN ('DRAFT', 'ANNOTATING', 'DISAGREEMENT', 'GOLD_READY', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS idx_benchmark_problems_status_topic_created
    ON benchmark_problems (status, topic, created_at DESC);

-- 2. Gold Annotations
CREATE TABLE IF NOT EXISTS gold_annotations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    benchmark_problem_id UUID NOT NULL REFERENCES benchmark_problems(id) ON DELETE CASCADE,
    annotator_reference VARCHAR(80) NOT NULL,
    gold_specification JSONB NOT NULL,
    annotation_status VARCHAR(24) NOT NULL,
    schema_catalog_checksum VARCHAR(120),
    prompt_version VARCHAR(120),
    model_version VARCHAR(120)
);

ALTER TABLE IF EXISTS gold_annotations
    ADD COLUMN IF NOT EXISTS schema_catalog_checksum VARCHAR(120),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(120),
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(120);

-- 3. Benchmark Adjudications
CREATE TABLE IF NOT EXISTS benchmark_adjudications (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    benchmark_problem_id UUID NOT NULL REFERENCES benchmark_problems(id) ON DELETE CASCADE,
    reviewer_reference VARCHAR(80) NOT NULL,
    resolved_specification JSONB NOT NULL,
    disagreement_state VARCHAR(24) NOT NULL,
    rationale TEXT
);

ALTER TABLE IF EXISTS benchmark_adjudications
    ADD COLUMN IF NOT EXISTS rationale TEXT;

-- 4. Evaluation Runs
CREATE TABLE IF NOT EXISTS evaluation_runs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    benchmark_count INTEGER NOT NULL,
    evaluation_type VARCHAR(80) NOT NULL,
    report TEXT,
    metrics JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    requested_by_reference VARCHAR(80),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,
    benchmark_snapshot_hash VARCHAR(64),
    configuration JSONB,
    failure_code VARCHAR(64),
    failure_message TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE IF EXISTS evaluation_runs
    DROP CONSTRAINT IF EXISTS ck_evaluation_runs_status;
ALTER TABLE IF EXISTS evaluation_runs
    ADD CONSTRAINT ck_evaluation_runs_status
    CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_evaluation_runs_status_created
    ON evaluation_runs (status, created_at DESC);

-- 5. Law Gaps
CREATE TABLE IF NOT EXISTS law_gaps (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    gap_key VARCHAR(240) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    schema_id VARCHAR(80),
    topic VARCHAR(200),
    role VARCHAR(80),
    unit VARCHAR(40),
    label TEXT,
    occurrences INTEGER NOT NULL DEFAULT 1,
    example_description TEXT,
    example_formula TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    note TEXT,
    CONSTRAINT law_gaps_key_unique UNIQUE (gap_key),
    CONSTRAINT law_gaps_kind_check CHECK (kind IN ('UNCHECKED', 'NO_ROLE', 'AMBIGUOUS')),
    CONSTRAINT law_gaps_status_check CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX IF NOT EXISTS idx_law_gaps_status_occurrences
    ON law_gaps (status, occurrences DESC);
