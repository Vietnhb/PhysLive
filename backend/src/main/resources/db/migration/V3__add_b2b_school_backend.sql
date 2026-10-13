-- Establish the initial B2B school schema for existing PhysLive PostgreSQL databases.
-- Flyway applies this migration automatically during application startup.
BEGIN;

CREATE TABLE IF NOT EXISTS schools (
    id UUID PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE, code VARCHAR(80) NOT NULL UNIQUE,
    short_name VARCHAR(50), address TEXT, province_city VARCHAR(100), phone_number VARCHAR(20), contact_email VARCHAR(100),
    license_start DATE, license_end DATE, active BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_token_quota INTEGER DEFAULT 0 CHECK (monthly_token_quota >= 0),
    student_quota INTEGER CHECK (student_quota >= 0), plan_code VARCHAR(255), annual_price_vnd BIGINT,
    next_plan_code VARCHAR(255), used_tokens BIGINT DEFAULT 0 CHECK (used_tokens >= 0), token_usage_month DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (license_start IS NULL OR license_end IS NULL OR license_start <= license_end)
);
ALTER TABLE users ADD COLUMN IF NOT EXISTS school_id UUID REFERENCES schools(id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_by INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_users_school ON users(school_id);

INSERT INTO roles(name) VALUES ('ADMIN'), ('CONTENT_REVIEWER'), ('SCHOOL_MANAGER'), ('TEACHER'), ('STUDENT')
ON CONFLICT(name) DO NOTHING;
-- Keep REVIEWER as a supported compatibility alias. Do not assign unknown legacy schools.
UPDATE users u SET school_id = s.id FROM schools s
WHERE u.school_id IS NULL AND u.institution_id = s.id::text
AND u.role_id IN (SELECT id FROM roles WHERE name IN ('SCHOOL_MANAGER','TEACHER','STUDENT'));

CREATE TABLE IF NOT EXISTS school_classes (
    id UUID PRIMARY KEY, school_id UUID NOT NULL REFERENCES schools(id), name VARCHAR(100) NOT NULL,
    grade_level INTEGER NOT NULL, school_year VARCHAR(20) NOT NULL, subject VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_school_class_name_year ON school_classes(school_id, lower(name), school_year);
CREATE TABLE IF NOT EXISTS class_teacher_assignments (
    id UUID PRIMARY KEY, class_id UUID NOT NULL REFERENCES school_classes(id), teacher_id INTEGER NOT NULL REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE, assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_teacher_class UNIQUE(class_id, teacher_id)
);
CREATE TABLE IF NOT EXISTS class_enrollments (
    id UUID PRIMARY KEY, class_id UUID NOT NULL REFERENCES school_classes(id), student_id INTEGER NOT NULL REFERENCES users(id),
    school_year VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK(status IN ('ACTIVE','TRANSFERRED','DROPPED','COMPLETED')),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_active_enrollment_per_year
    ON class_enrollments(student_id, school_year) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS license_plans (
    code VARCHAR(40) PRIMARY KEY, name VARCHAR(255) NOT NULL, description VARCHAR(255) NOT NULL,
    annual_price_vnd BIGINT NOT NULL CHECK(annual_price_vnd > 0), student_quota INTEGER NOT NULL CHECK(student_quota > 0),
    monthly_token_quota INTEGER CHECK(monthly_token_quota >= 0), active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS school_payments (
    id UUID PRIMARY KEY, school_id UUID NOT NULL REFERENCES schools(id), manager_id INTEGER NOT NULL REFERENCES users(id),
    plan_code VARCHAR(255) NOT NULL, amount_vnd BIGINT NOT NULL CHECK(amount_vnd > 0), monthly_token_quota INTEGER,
    student_quota INTEGER, purpose VARCHAR(255) NOT NULL DEFAULT 'REGISTRATION', annual_price_vnd BIGINT,
    previous_plan_code VARCHAR(255), license_start DATE, license_end DATE, status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), paid_at TIMESTAMPTZ, provider_transaction_no VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_school_payment_created ON school_payments(school_id, created_at DESC);
CREATE TABLE IF NOT EXISTS token_usage_audits (
    id UUID PRIMARY KEY, school_id UUID NOT NULL REFERENCES schools(id), user_id INTEGER NOT NULL REFERENCES users(id),
    tokens BIGINT NOT NULL CHECK(tokens >= 0), operation VARCHAR(64) NOT NULL, usage_month DATE NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_token_audit_school_time ON token_usage_audits(school_id, recorded_at DESC);

-- Enforce new/changed role-school assignments, without guessing a school for legacy accounts.
CREATE OR REPLACE FUNCTION physlive_check_role_school() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE role_name TEXT;
BEGIN
    SELECT name INTO role_name FROM roles WHERE id = NEW.role_id;
    IF role_name IN ('ADMIN','CONTENT_REVIEWER','REVIEWER') AND NEW.school_id IS NULL THEN RETURN NEW; END IF;
    IF role_name IN ('SCHOOL_MANAGER','TEACHER','STUDENT') AND NEW.school_id IS NOT NULL THEN RETURN NEW; END IF;
    RAISE EXCEPTION 'Role and school assignment are inconsistent';
END $$;
DROP TRIGGER IF EXISTS check_role_school_consistency ON users;
CREATE TRIGGER check_role_school_consistency BEFORE INSERT OR UPDATE OF role_id, school_id ON users
    FOR EACH ROW EXECUTE FUNCTION physlive_check_role_school();
DO $$
DECLARE manager_role INTEGER;
BEGIN
    SELECT id INTO manager_role FROM roles WHERE name = 'SCHOOL_MANAGER';
    EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS idx_one_school_manager_per_school ON users(school_id) WHERE role_id = %s AND active = true', manager_role);
END $$;
COMMIT;
