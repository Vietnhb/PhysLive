-- One localized display-name column. Runtime identities continue to use slug/schema codes.
ALTER TABLE topics ALTER COLUMN name TYPE jsonb
USING jsonb_build_object('en', name, 'vi', name);

-- Seed translations for the existing catalogue once; future names are entered in the management UI.
UPDATE topics AS topic
SET name = jsonb_set(topic.name, '{vi}', to_jsonb(translation.vi_name)), updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('kinematics', 'Động học'),
    ('dynamics', 'Động lực học'),
    ('work-energy-power', 'Công, năng lượng và công suất'),
    ('momentum', 'Động lượng'),
    ('circular-motion', 'Chuyển động tròn'),
    ('solid-deformation', 'Biến dạng của vật rắn'),
    ('earth-and-sky', 'Trái Đất và bầu trời'),
    ('gravitational-field', 'Trường hấp dẫn'),
    ('oscillations', 'Dao động'),
    ('waves', 'Sóng'),
    ('radio-communication', 'Truyền thông bằng sóng vô tuyến'),
    ('electric-field', 'Điện trường'),
    ('electric-current', 'Dòng điện và mạch điện'),
    ('electronics', 'Điện tử học'),
    ('thermal-physics', 'Vật lý nhiệt'),
    ('ideal-gas', 'Khí lý tưởng'),
    ('magnetic-field', 'Từ trường'),
    ('alternating-current', 'Dòng điện xoay chiều'),
    ('nuclear-physics', 'Vật lý hạt nhân'),
    ('medical-physics', 'Vật lý y học'),
    ('quantum-physics', 'Vật lý lượng tử')
) AS translation(slug, vi_name)
WHERE topic.slug = translation.slug AND topic.name->>'en' = upper(replace(topic.slug, '-', '_'));

ALTER TABLE topics ADD CONSTRAINT ck_topics_localized_name CHECK (
    jsonb_typeof(name) = 'object'
    AND name ? 'en' AND name ? 'vi'
    AND jsonb_typeof(name->'en') = 'string' AND jsonb_typeof(name->'vi') = 'string'
    AND length(btrim(name->>'en')) BETWEEN 1 AND 80
    AND length(btrim(name->>'vi')) BETWEEN 1 AND 80
);
