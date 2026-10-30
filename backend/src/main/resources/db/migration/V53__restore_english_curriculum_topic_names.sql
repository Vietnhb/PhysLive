-- Keep machine names in the database; Vietnamese labels belong to the frontend.
-- Correct V52 without changing IDs, slugs, enabled flags or any linked content.
UPDATE topics AS topic
SET name = upper(replace(topic.slug, '-', '_')), updated_at = CURRENT_TIMESTAMP
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
WHERE topic.slug = translation.slug AND topic.name = translation.vi_name;
