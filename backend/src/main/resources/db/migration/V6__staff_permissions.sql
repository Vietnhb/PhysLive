-- Individual STAFF capabilities. Existing STAFF accounts retain teaching access by default.
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    role_id INTEGER NOT NULL REFERENCES roles(id),
    label VARCHAR(120) NOT NULL
);

INSERT INTO permissions (code, role_id, label)
SELECT v.code, roles.id, v.label
FROM (VALUES
    (1, 'TEACH', 'Giáo viên'),
    (2, 'DEPARTMENT_HEAD_PHYSICS', 'Tổ trưởng bộ môn Vật Lý')
) AS v(position, code, label)
JOIN roles ON roles.name = 'STAFF'
ORDER BY v.position;

CREATE TABLE user_permissions (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(id),
    granted_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, permission_id)
);
CREATE INDEX idx_user_permissions_permission ON user_permissions(permission_id);

INSERT INTO user_permissions (user_id, permission_id)
SELECT users.id, permissions.id
FROM users
JOIN roles ON roles.id = users.role_id AND roles.name = 'STAFF'
JOIN permissions ON permissions.code = 'TEACH';

-- A permission can only be granted to a user whose role owns that permission.
CREATE OR REPLACE FUNCTION validate_user_permission() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM users JOIN permissions ON permissions.role_id = users.role_id
        WHERE users.id = NEW.user_id AND permissions.id = NEW.permission_id
    ) THEN
        RAISE EXCEPTION 'Permission does not belong to the role of this user' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER check_user_permission_role BEFORE INSERT OR UPDATE ON user_permissions
FOR EACH ROW EXECUTE FUNCTION validate_user_permission();

-- Checked at commit so role and grant changes can be persisted in either SQL order.
CREATE OR REPLACE FUNCTION validate_staff_permission_set() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target INTEGER; role_name TEXT;
BEGIN
    IF TG_TABLE_NAME = 'users' THEN target := NEW.id; ELSE target := OLD.user_id; END IF;
    SELECT roles.name INTO role_name FROM users JOIN roles ON roles.id = users.role_id WHERE users.id = target;
    IF role_name IS NULL THEN RETURN NULL; END IF;
    IF EXISTS (
        SELECT 1 FROM user_permissions
        JOIN permissions ON permissions.id = user_permissions.permission_id
        JOIN users ON users.id = user_permissions.user_id
        WHERE user_permissions.user_id = target AND permissions.role_id <> users.role_id
    ) THEN
        RAISE EXCEPTION 'User % holds a permission of another role', target USING ERRCODE = '23514';
    END IF;
    IF role_name = 'STAFF'
       AND NOT EXISTS (SELECT 1 FROM user_permissions WHERE user_id = target) THEN
        RAISE EXCEPTION 'STAFF account % needs at least one permission', target USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER check_staff_permission_set_on_users
AFTER INSERT OR UPDATE OF role_id ON users DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_staff_permission_set();
CREATE CONSTRAINT TRIGGER check_staff_permission_set_on_grants
AFTER DELETE ON user_permissions DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_staff_permission_set();

COMMENT ON TABLE permissions IS 'STAFF capabilities: TEACH and DEPARTMENT_HEAD_PHYSICS';
COMMENT ON TABLE user_permissions IS 'Audited N-N permission grants; granted_by is the SCHOOL or MANAGER actor';
