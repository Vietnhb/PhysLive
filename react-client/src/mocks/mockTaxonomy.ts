import type { Topic, Module, Level, ClassLesson, Lesson } from '../types/database';

export const MOCK_TOPICS: Topic[] = [
  {
    id: 'top-kinematics',
    code: 'KINEMATICS',
    name: 'Động học chất điểm (Vật lí 10)',
    description: 'Chuyển động thẳng biến đổi đều, rơi tự do, chuyển động ném ngang và ném xiên (SGK Vật lí 10).',
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'top-dynamics',
    code: 'DYNAMICS',
    name: 'Động lực học chất điểm (Vật lí 10)',
    description: 'Ba định luật Newton, lực ma sát trượt, chuyển động trên mặt phẳng nghiêng và năng lượng (SGK Vật lí 10).',
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'top-circuits',
    code: 'CIRCUITS',
    name: 'Dòng điện & Mạch điện (Vật lí 11)',
    description: 'Định luật Coulomb, điện trường, tụ điện và định luật Ohm cho toàn mạch (SGK Vật lí 11).',
    created_at: '2026-09-01T00:00:00Z',
  },
];

export const MOCK_MODULES: Module[] = [
  // Kinematics modules
  {
    id: 'mod-kin-1',
    topic_id: 'top-kinematics',
    name: 'Chuyển động thẳng biến đổi đều',
    description: 'Vận tốc, gia tốc, quãng đường, phương trình toạ độ x(t)',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'mod-kin-2',
    topic_id: 'top-kinematics',
    name: 'Chuyển động ném ngang & ném xiên',
    description: 'Khảo sát quỹ đạo parabol, tầm xa x_max, độ cao cực đại h_max',
    order_index: 2,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  // Dynamics modules
  {
    id: 'mod-dyn-1',
    topic_id: 'top-dynamics',
    name: 'Các định luật Newton & Lực cơ học',
    description: 'Lực ma sát, lực đàn hồi, phân tích tổng hợp lực F = m.a',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'mod-dyn-2',
    topic_id: 'top-dynamics',
    name: 'Chuyển động trên mặt phẳng nghiêng',
    description: 'Phân tích trọng lực trên hệ toạ độ Oxy mặt phẳng nghiêng, phản lực N',
    order_index: 2,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  // Circuits modules
  {
    id: 'mod-cir-1',
    topic_id: 'top-circuits',
    name: 'Định luật Ôm cho toàn mạch',
    description: 'Nguồn điện, suất điện động, điện trở trong, mạch cầu nối tiếp và song song',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
];

export const MOCK_LESSONS: Lesson[] = [
  {
    id: 'les-proj-1',
    module_id: 'mod-kin-2',
    name: 'Bài toán ném xiên từ độ cao h với vận tốc đầu v0',
    grade_level: 10,
    description: 'Phân tích chuyển động ném xiên theo hai trục Ox và Oy',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'les-incline-1',
    module_id: 'mod-dyn-2',
    name: 'Vật trượt trên mặt phẳng nghiêng có ma sát',
    grade_level: 10,
    description: 'Tính gia tốc trượt và vận tốc ở chân dốc với góc nghiêng alpha và hệ số ma sát mu',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'les-circuit-1',
    module_id: 'mod-cir-1',
    name: 'Mạch điện gồm nguồn E, r nối tiếp điện trở R',
    grade_level: 11,
    description: 'Tính cường độ dòng điện trong mạch I = E / (R + r) và công suất tiêu thụ',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
];

export const MOCK_LEVELS: Level[] = [
  {
    id: 'lvl-kin-10',
    module_id: 'mod-kin-2',
    name: 'Khối 10 — Động học cơ bản & nâng cao',
    order_index: 1,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'lvl-dyn-10',
    module_id: 'mod-dyn-2',
    name: 'Khối 10 — Động lực học & Ma sát',
    order_index: 2,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'lvl-cir-11',
    module_id: 'mod-cir-1',
    name: 'Khối 11 — Dòng điện & Mạch điện',
    order_index: 3,
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
];

export const MOCK_CLASS_LESSONS: ClassLesson[] = [
  {
    id: 'cls-proj-10a1',
    level_id: 'lvl-kin-10',
    teacher_id: 'u-teacher-hung',
    name: 'Tiết 12: Chuyển động ném xiên và ném ngang từ độ cao h',
    class_name: 'Lớp 10A1 — THPT Chuyên KHTN',
    explanation_notes: 'Trong bài học trước, thầy đã hướng dẫn phân tích chuyển động ném xiên thành hai thành phần độc lập: Ox chuyển động thẳng đều (vx = v0*cos(alpha)) và Oy chuyển động biến đổi đều với gia tốc g (vy = v0*sin(alpha) - g*t). Chú ý khi vật chạm đất thì toạ độ y = 0, giải phương trình bậc hai để tìm thời gian bay t_flight!',
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'cls-car-10a1',
    level_id: 'lvl-kin-10',
    teacher_id: 'u-teacher-hung',
    name: 'Tiết 14: Chuyển động thẳng biến đổi đều — Bài toán phanh xe ô tô',
    class_name: 'Lớp 10A1 — THPT Chuyên KHTN',
    explanation_notes: 'Lưu ý khi ô tô hãm phanh: Vận tốc ban đầu thường cho dạng km/h phải lập tức đổi sang m/s (chia 3.6). Gia tốc hãm phanh mang dấu âm (a < 0). Khi xe dừng hẳn thì vận tốc tức thời v = 0. Sử dụng hệ thức độc lập thời gian v^2 - v0^2 = 2*a*s để tính nhanh quãng đường phanh!',
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  },
  {
    id: 'cls-incline-10a2',
    level_id: 'lvl-dyn-10',
    teacher_id: 'u-teacher-hung',
    name: 'Tiết 19: Khảo sát chuyển động của vật trên mặt phẳng nghiêng có ma sát',
    class_name: 'Lớp 10A2 — THPT Chuyên KHTN',
    explanation_notes: 'Các em nhớ vẽ hình phân tích đủ 3 lực: Trọng lực P, Phản lực pháp tuyến N và Lực ma sát trượt F_ms = mu * N. Gia tốc trượt a = g*(sin(alpha) - mu*cos(alpha)). Điều kiện để vật tự trượt là góc dốc tan(alpha) > mu!',
    is_active: true,
    created_at: '2026-09-01T00:00:00Z',
  }
];
