-- Translate display names only. Slugs and schema/specification topic codes stay unchanged.
UPDATE topics AS topic
SET name = translation.vi_name, updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('kinematics', 'KINEMATICS', 'Động học'),
    ('dynamics', 'DYNAMICS', 'Động lực học'),
    ('work-energy-power', 'WORK_ENERGY_POWER', 'Công, năng lượng và công suất'),
    ('momentum', 'MOMENTUM', 'Động lượng'),
    ('circular-motion', 'CIRCULAR_MOTION', 'Chuyển động tròn'),
    ('solid-deformation', 'SOLID_DEFORMATION', 'Biến dạng của vật rắn'),
    ('earth-and-sky', 'EARTH_AND_SKY', 'Trái Đất và bầu trời'),
    ('gravitational-field', 'GRAVITATIONAL_FIELD', 'Trường hấp dẫn'),
    ('oscillations', 'OSCILLATIONS', 'Dao động'),
    ('waves', 'WAVES', 'Sóng'),
    ('radio-communication', 'RADIO_COMMUNICATION', 'Truyền thông bằng sóng vô tuyến'),
    ('electric-field', 'ELECTRIC_FIELD', 'Điện trường'),
    ('electric-current', 'ELECTRIC_CURRENT', 'Dòng điện và mạch điện'),
    ('electronics', 'ELECTRONICS', 'Điện tử học'),
    ('thermal-physics', 'THERMAL_PHYSICS', 'Vật lý nhiệt'),
    ('ideal-gas', 'IDEAL_GAS', 'Khí lý tưởng'),
    ('magnetic-field', 'MAGNETIC_FIELD', 'Từ trường'),
    ('alternating-current', 'ALTERNATING_CURRENT', 'Dòng điện xoay chiều'),
    ('nuclear-physics', 'NUCLEAR_PHYSICS', 'Vật lý hạt nhân'),
    ('medical-physics', 'MEDICAL_PHYSICS', 'Vật lý y học'),
    ('quantum-physics', 'QUANTUM_PHYSICS', 'Vật lý lượng tử')
) AS translation(slug, old_name, vi_name)
WHERE topic.slug = translation.slug AND topic.name = translation.old_name;
