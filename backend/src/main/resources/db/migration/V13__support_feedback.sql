CREATE TABLE support_items (
    id UUID PRIMARY KEY,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('FEEDBACK', 'MESSAGE')),
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    subject VARCHAR(180) NOT NULL CHECK (length(btrim(subject)) > 0),
    content TEXT NOT NULL CHECK (length(btrim(content)) > 0),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'READ', 'RESOLVED')),
    admin_response TEXT,
    responded_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX support_items_sender_created_idx
    ON support_items (sender_id, created_at DESC);

CREATE INDEX support_items_kind_created_idx
    ON support_items (kind, created_at DESC);
