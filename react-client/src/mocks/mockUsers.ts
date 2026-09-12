import type { User } from '../types/database';

export const MOCK_USERS: User[] = [
  {
    id: 'u-teacher-hung',
    role_id: 'r-teacher',
    role: 'TEACHER',
    email: 'hung.teacher@physlive.edu.vn',
    full_name: 'Thầy Nguyễn Thế Hùng',
    school_name: 'THPT Chuyên Khoa Học Tự Nhiên',
    status: 'active',
    created_at: '2026-09-01T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  },
  {
    id: 'u-student-nam',
    role_id: 'r-student',
    role: 'STUDENT',
    email: 'nam.student@physlive.edu.vn',
    full_name: 'Trần Hoài Nam (Lớp 10A1)',
    school_name: 'THPT Chuyên Khoa Học Tự Nhiên',
    status: 'active',
    created_at: '2026-09-05T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  },
  {
    id: 'u-reviewer-kiet',
    role_id: 'r-reviewer',
    role: 'REVIEWER',
    email: 'kiet.reviewer@physlive.edu.vn',
    full_name: 'Tuấn Kiệt (Physics Reviewer)',
    school_name: 'Ban Thẩm Định Học Liệu PhysLive',
    status: 'active',
    created_at: '2026-08-20T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  },
  {
    id: 'u-admin-viet',
    role_id: 'r-admin',
    role: 'ADMIN',
    email: 'admin@physlive.edu.vn',
    full_name: 'Quản Trị Viên Hệ Thống',
    status: 'active',
    created_at: '2026-08-01T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  }
];

export const CURRENT_USER_KEY = 'physlive_current_user';

export const getStoredUser = (): User => {
  const stored = localStorage.getItem(CURRENT_USER_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      // Fallback
    }
  }
  return MOCK_USERS[0]; // Mặc định là Thầy Hùng
};

export const setStoredUser = (user: User) => {
  localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
};
