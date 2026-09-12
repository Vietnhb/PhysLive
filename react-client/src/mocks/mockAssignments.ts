import type { Assignment, StudentPractice } from '../types/database';

export const MOCK_ASSIGNMENTS: Assignment[] = [
  {
    id: 'asg-001',
    simulation_id: 'sim-proj-001',
    teacher_id: 'u-teacher-hung',
    class_lesson_id: 'cls-proj-10a1',
    title: 'Thử thách 01: Dự đoán tầm xa cực đại của vật ném xiên',
    instructions: 'Đọc kỹ giải thích bài học trước của thầy, tính toán lý thuyết và nhập số liệu dự đoán tầm xa L_max (m) của vật khi chạm đất trước khi khởi chạy mô phỏng đối chứng.',
    target_question: 'Tầm xa cực đại L_max (m) vật đạt được khi chạm đất là bao nhiêu?',
    target_quantity_symbol: 'L_max (m)',
    due_date: '2026-09-20T23:59:59Z',
    created_at: '2026-09-12T10:00:00Z',
  },
  {
    id: 'asg-002',
    simulation_id: 'sim-car-brake-003',
    teacher_id: 'u-teacher-hung',
    class_lesson_id: 'cls-car-10a1',
    title: 'Thử thách 02: Xác định quãng đường hãm phanh của xe ô tô',
    instructions: 'Xe đang chạy với tốc độ 72 km/h (20 m/s) thì hãm phanh với gia tốc a = -4 m/s². Hãy tính và dự đoán quãng đường xe đi được cho đến khi dừng lại hẳn.',
    target_question: 'Quãng đường phanh s (m) xe đi được đến khi dừng hẳn là bao nhiêu?',
    target_quantity_symbol: 's (m)',
    due_date: '2026-09-22T23:59:59Z',
    created_at: '2026-09-12T11:30:00Z',
  },
  {
    id: 'asg-003',
    simulation_id: 'sim-incline-002',
    teacher_id: 'u-teacher-hung',
    class_lesson_id: 'cls-incline-10a2',
    title: 'Thử thách 03: Tính gia tốc trượt dốc mặt phẳng nghiêng',
    instructions: 'Khối hộp khối lượng m = 2kg trượt trên dốc nghiêng 30 độ, hệ số ma sát mu = 0.1, g = 9.8 m/s². Hãy tính và dự đoán gia tốc trượt a (m/s²).',
    target_question: 'Gia tốc trượt a (m/s²) của khối hộp xuôi theo mặt dốc bằng bao nhiêu?',
    target_quantity_symbol: 'a (m/s²)',
    due_date: '2026-09-25T23:59:59Z',
    created_at: '2026-09-12T12:00:00Z',
  }
];

export const MOCK_STUDENT_PRACTICES: StudentPractice[] = [
  {
    id: 'prac-001',
    assignment_id: 'asg-001',
    student_id: 'u-student-nam',
    predicted_answer: '38.5',
    actual_answer: '38.45',
    replayed_presets: { v0: 15, alpha: 30, h: 20, g: 9.8 },
    is_correct: true,
    submitted_at: '2026-09-12T11:20:00Z',
  }
];
