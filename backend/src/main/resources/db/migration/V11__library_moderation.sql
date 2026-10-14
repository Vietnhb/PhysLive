ALTER TABLE library_items
    ADD COLUMN IF NOT EXISTS shared_institution_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS moderation_comment TEXT,
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS moderated_by INTEGER REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS record_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS library_moderation_audits (
    id UUID PRIMARY KEY,
    library_item_id UUID NOT NULL REFERENCES library_items(id) ON DELETE CASCADE,
    reviewer_id INTEGER NOT NULL REFERENCES users(id),
    from_status VARCHAR(16),
    to_status VARCHAR(16) NOT NULL,
    comment TEXT,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_library_moderation_queue
    ON library_items (visibility, moderation_status, created_at);

CREATE INDEX IF NOT EXISTS idx_library_moderation_audit_item
    ON library_moderation_audits (library_item_id, decided_at DESC);
