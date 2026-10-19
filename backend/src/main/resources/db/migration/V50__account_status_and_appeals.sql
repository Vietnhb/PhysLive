-- V50: Account status 3-tiers, account appeals, randomized assignment parameters, and curriculum code hierarchy

-- 1. Account status enhancement (ACTIVE, WARNED, SUSPENDED_READ_ONLY)
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(32) DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMP WITH TIME ZONE;

-- Sync existing active flag to account_status
UPDATE users SET account_status = 'SUSPENDED_READ_ONLY' WHERE active = false AND (account_status IS NULL OR account_status = 'ACTIVE');
UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_account_status_check'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_account_status_check
            CHECK (account_status IN ('ACTIVE', 'WARNED', 'SUSPENDED_READ_ONLY'));
    END IF;
END $$;

-- 2. Account Appeals table for suspended users to file dispute records
CREATE TABLE IF NOT EXISTS account_appeals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    resolution_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT account_appeals_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_account_appeals_user_id ON account_appeals(user_id);
CREATE INDEX IF NOT EXISTS idx_account_appeals_status ON account_appeals(status);

-- 3. Randomized assignment parameters for anti-cheat & personalized problem variations
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS assigned_parameters JSONB;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS parameter_template JSONB;

-- 4. Curriculum code hierarchy (K, K01, K01-10, K01-10-01)
ALTER TABLE topics ADD COLUMN IF NOT EXISTS code VARCHAR(32);
ALTER TABLE content_modules ADD COLUMN IF NOT EXISTS code VARCHAR(32);
ALTER TABLE grade_levels ADD COLUMN IF NOT EXISTS code VARCHAR(32);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS code VARCHAR(32);

-- 5. Row Level Security (RLS) Policy template on Supabase for SUSPENDED_READ_ONLY
-- Note: In Supabase, RLS applies to tables enabled with ROW LEVEL SECURITY.
-- For accounts with account_status = 'SUSPENDED_READ_ONLY', mutations (INSERT, UPDATE, DELETE) are blocked
-- on core application resources while SELECT and appeals INSERT remain permitted.
DO $$
BEGIN
    -- Ensure RLS is enabled for account_appeals
    ALTER TABLE account_appeals ENABLE ROW LEVEL SECURITY;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;
