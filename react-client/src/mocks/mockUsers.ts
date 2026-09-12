import type { User } from '../types/database';
import { getToken } from '../utils/token';
import { isTokenExpired } from '../utils/jwt';

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
    full_name: 'Nguyễn Hoàng Bảo Việt (System Admin)',
    school_name: 'Ban Điều Hành Kỹ Thuật PhysLive',
    status: 'active',
    created_at: '2026-08-01T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  },
  {
    id: 'u-teacher-ha',
    role_id: 'r-teacher',
    role: 'TEACHER',
    email: 'ha.teacher@physlive.edu.vn',
    full_name: 'Cô Trần Thu Hà',
    school_name: 'THPT Chuyên Hà Nội - Amsterdam',
    status: 'active',
    created_at: '2026-08-25T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  },
  {
    id: 'u-student-mai',
    role_id: 'r-student',
    role: 'STUDENT',
    email: 'mai.student@physlive.edu.vn',
    full_name: 'Lê Thị Mai (Lớp 11 Lý)',
    school_name: 'THPT Chuyên Lê Hồng Phong TP.HCM',
    status: 'disabled',
    created_at: '2026-09-02T08:00:00Z',
    updated_at: '2026-09-11T14:30:00Z',
  },
  {
    id: 'u-student-phong',
    role_id: 'r-student',
    role: 'STUDENT',
    email: 'phong.student@physlive.edu.vn',
    full_name: 'Nguyễn Hoàng Phong (Lớp 10B2)',
    school_name: 'THPT Marie Curie',
    status: 'active',
    created_at: '2026-09-08T08:00:00Z',
    updated_at: '2026-09-12T08:00:00Z',
  }
];

export const CURRENT_USER_KEY = 'physlive_current_user';
export const ADMIN_USERS_KEY = 'physlive_admin_users';

export const getStoredUser = (): User | null => {
  const token = getToken();
  // Nếu không có token hoặc token đã hết hạn, không tự động đăng nhập
  if (!token || isTokenExpired(token)) {
    return null;
  }

  const stored = localStorage.getItem(CURRENT_USER_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }
  return null;
};

export const setStoredUser = (user: User | null) => {
  if (user) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(CURRENT_USER_KEY);
  }
};

export const getAdminUsers = (): User[] => {
  const stored = localStorage.getItem(ADMIN_USERS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return MOCK_USERS;
    }
  }
  return MOCK_USERS;
};

export const setAdminUsers = (users: User[]): void => {
  localStorage.setItem(ADMIN_USERS_KEY, JSON.stringify(users));
};

export const disableUserInStorage = (userId: string): User[] => {
  const users = getAdminUsers();
  const updated = users.map(u => u.id === userId ? { ...u, status: 'disabled' as const, updated_at: new Date().toISOString() } : u);
  setAdminUsers(updated);
  return updated;
};

export const restoreUserInStorage = (userId: string): User[] => {
  const users = getAdminUsers();
  const updated = users.map(u => u.id === userId ? { ...u, status: 'active' as const, updated_at: new Date().toISOString() } : u);
  setAdminUsers(updated);
  return updated;
};

export const changeUserRoleInStorage = (userId: string, newRole: User['role']): User[] => {
  const users = getAdminUsers();
  const updated = users.map(u => u.id === userId ? {
    ...u,
    role: newRole,
    role_id: `r-${newRole.toLowerCase()}`,
    updated_at: new Date().toISOString()
  } : u);
  setAdminUsers(updated);
  return updated;
};

