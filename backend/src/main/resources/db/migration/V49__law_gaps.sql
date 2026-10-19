-- What teachers' simulations needed that the law catalogues could not check: one row per kind of gap, counted.
CREATE TABLE IF NOT EXISTS law_gaps (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
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
CREATE INDEX IF NOT EXISTS idx_law_gaps_status_occurrences ON law_gaps (status, occurrences DESC);
