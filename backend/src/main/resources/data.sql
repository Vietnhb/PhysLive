-- ============================================================
-- PhysLive – seed / migration script
-- Chỉ chứa DML (INSERT) và migration an toàn (ALTER ... IF NOT EXISTS)
-- Spring Boot chạy file này mỗi lần khởi động → phải idempotent
-- ============================================================

-- 1. Seed roles (idempotent)
INSERT INTO public.roles (name) VALUES
    ('ADMIN'),
    ('TEACHER'),
    ('REVIEWER'),
    ('STUDENT'),
    ('GUEST')
ON CONFLICT (name) DO NOTHING;