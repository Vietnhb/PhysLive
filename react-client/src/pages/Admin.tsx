import React, { useEffect, useState, useMemo } from 'react';
import type { User, RoleType } from '../types/database';
import { RoleBadge } from '../components/common/RoleBadge';
import { useAuth } from '../context/AuthContext';
import {
  getUsers,
  disableUserApi,
  restoreUserApi,
  changeUserRoleApi,
} from '../api/userApi';
import { setAdminUsers, getAdminUsers } from '../mocks/mockUsers';
import {
  Users,
  GraduationCap,
  BookOpen,
  ShieldCheck,
  UserX,
  Search,
  RefreshCw,
  UserPlus,
  Eye,
  RotateCcw,
  ShieldAlert,
} from 'lucide-react';

export const Admin: React.FC = () => {
  const { currentUser } = useAuth();

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<string>('ALL');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState<string>('ALL');

  // Native Card View / Edit state (Section 6.3)
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [editFullName, setEditFullName] = useState('');
  const [editRole, setEditRole] = useState<RoleType>('STUDENT');
  const [editSchool, setEditSchool] = useState('');

  // Native Card Create state
  const [isCreating, setIsCreating] = useState(false);
  const [newFullName, setNewFullName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newRole, setNewRole] = useState<RoleType>('STUDENT');
  const [newSchool, setNewSchool] = useState('THPT Chuyên Khoa Học Tự Nhiên');

  // Notification Toast
  const [toastMessage, setToastMessage] = useState<{ type: 'success' | 'danger' | 'info'; text: string } | null>(null);

  const showToast = (text: string, type: 'success' | 'danger' | 'info' = 'success') => {
    setToastMessage({ type, text });
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Tải danh sách người dùng
  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await getUsers();
      setUsers(data);
    } catch {
      showToast('Không thể kết nối API máy chủ, đã tải dữ liệu dự phòng.', 'info');
      setUsers(getAdminUsers());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  // Lọc và tìm kiếm
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchesSearch =
        u.full_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (u.school_name && u.school_name.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesRole =
        selectedRoleFilter === 'ALL' || u.role === selectedRoleFilter;

      const matchesStatus =
        selectedStatusFilter === 'ALL' || u.status === selectedStatusFilter;

      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [users, searchTerm, selectedRoleFilter, selectedStatusFilter]);

  // Thống kê nhanh
  const stats = useMemo(() => {
    const total = users.length;
    const teachers = users.filter((u) => u.role === 'TEACHER').length;
    const students = users.filter((u) => u.role === 'STUDENT').length;
    const reviewers = users.filter((u) => u.role === 'REVIEWER').length;
    const disabled = users.filter((u) => u.status === 'disabled').length;
    return { total, teachers, students, reviewers, disabled };
  }, [users]);

  // Bắt đầu chỉnh sửa người dùng dạng Native Card (Sec 6.3)
  const handleStartEdit = (user: User) => {
    setIsCreating(false);
    setEditingUser(user);
    setEditFullName(user.full_name);
    setEditRole(user.role);
    setEditSchool(user.school_name || '');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCancelEdit = () => {
    setEditingUser(null);
    setIsCreating(false);
  };

  // Lưu chỉnh sửa người dùng dạng Native Card
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;

    if (!editFullName.trim()) {
      showToast('Họ và tên không được để trống', 'danger');
      return;
    }

    try {
      let nextRoleList = users;
      if (editRole !== editingUser.role) {
        nextRoleList = await changeUserRoleApi(editingUser.id, editRole);
      }

      const updatedUsers = nextRoleList.map((u) =>
        u.id === editingUser.id
          ? {
              ...u,
              full_name: editFullName.trim(),
              school_name: editSchool.trim(),
              role: editRole,
              role_id: `r-${editRole.toLowerCase()}`,
              updated_at: new Date().toISOString(),
            }
          : u
      );

      setUsers(updatedUsers);
      setAdminUsers(updatedUsers);
      setEditingUser(null);
      showToast(`Đã lưu cập nhật thông tin cho "${editFullName.trim()}"`, 'success');
    } catch {
      showToast('Lỗi khi lưu thông tin người dùng', 'danger');
    }
  };

  // 1. Vô hiệu hóa người dùng (Disable User)
  const handleDisableUser = async (user: User) => {
    if (user.id === currentUser?.id) {
      alert('Bạn không thể tự vô hiệu hóa tài khoản quản trị hiện tại của chính mình!');
      return;
    }
    const confirm = window.confirm(`Bạn có chắc muốn KHÓA (Vô hiệu hóa) tài khoản "${user.full_name}" (${user.email})?`);
    if (!confirm) return;

    try {
      const updatedList = await disableUserApi(user.id);
      setUsers(updatedList);
      if (editingUser?.id === user.id) {
        setEditingUser({ ...editingUser, status: 'disabled' });
      }
      showToast(`Đã khóa thành công tài khoản: ${user.full_name}`, 'danger');
    } catch {
      showToast('Lỗi khi khóa tài khoản người dùng', 'danger');
    }
  };

  // 2. Khôi phục người dùng (Restore User)
  const handleRestoreUser = async (user: User) => {
    const confirm = window.confirm(`Khôi phục tài khoản "${user.full_name}" trở lại trạng thái hoạt động bình thường?`);
    if (!confirm) return;

    try {
      const updatedList = await restoreUserApi(user.id);
      setUsers(updatedList);
      if (editingUser?.id === user.id) {
        setEditingUser({ ...editingUser, status: 'active' });
      }
      showToast(`Đã khôi phục hoạt động cho tài khoản: ${user.full_name}`, 'success');
    } catch {
      showToast('Lỗi khi khôi phục người dùng', 'danger');
    }
  };

  // 3. Đổi vai trò (Change Role) trực tiếp trên table
  const handleChangeRole = async (user: User, nextRole: RoleType) => {
    if (user.role === nextRole) return;
    if (user.id === currentUser?.id && nextRole !== 'ADMIN') {
      const confirmSelf = window.confirm('Cảnh báo: Bạn đang hạ quyền tài khoản Admin của chính mình! Bạn có chắc không?');
      if (!confirmSelf) return;
    }

    try {
      const updatedList = await changeUserRoleApi(user.id, nextRole);
      setUsers(updatedList);
      if (editingUser?.id === user.id) {
        setEditingUser({ ...editingUser, role: nextRole, role_id: `r-${nextRole.toLowerCase()}` });
      }
      showToast(`Đã cập nhật vai trò của "${user.full_name}" thành: ${nextRole}`, 'success');
    } catch {
      showToast('Lỗi khi đổi vai trò người dùng', 'danger');
    }
  };

  // 4. Thêm tài khoản mới (Create User)
  const handleCreateUser = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFullName.trim() || !newEmail.trim()) {
      showToast('Vui lòng nhập đầy đủ họ tên và email!', 'danger');
      return;
    }

    const newUser: User = {
      id: `u-${newRole.toLowerCase()}-${Date.now().toString(36)}`,
      role_id: `r-${newRole.toLowerCase()}`,
      role: newRole,
      email: newEmail.trim(),
      full_name: newFullName.trim(),
      school_name: newSchool.trim(),
      status: 'active',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    const nextUsers = [newUser, ...users];
    setUsers(nextUsers);
    setAdminUsers(nextUsers);
    showToast(`Tạo thành công tài khoản mới: ${newUser.full_name} (${newUser.role})`, 'success');

    // Reset form
    setNewFullName('');
    setNewEmail('');
    setNewRole('STUDENT');
    setIsCreating(false);
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Toast Notification */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            top: '24px',
            right: '24px',
            zIndex: 9999,
            padding: '14px 20px',
            borderRadius: '10px',
            backgroundColor:
              toastMessage.type === 'success' ? '#065f46' : toastMessage.type === 'danger' ? '#991b1b' : '#1e3a8a',
            color: '#ffffff',
            fontWeight: 600,
            fontSize: '14px',
            boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            animation: 'fadeIn 0.2s ease',
          }}
        >
          <span>{toastMessage.type === 'success' ? '✓' : toastMessage.type === 'danger' ? '🚫' : 'ℹ️'}</span>
          <span>{toastMessage.text}</span>
        </div>
      )}

      {/* Header Banner */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          backgroundColor: 'var(--bg-card)',
          padding: '24px',
          borderRadius: '16px',
          border: '1px solid var(--border-subtle)',
          boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <ShieldAlert size={26} color="#dc2626" />
            <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>
              Quản Trị Người Dùng & Phân Quyền Hệ Thống (RBAC)
            </h1>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                backgroundColor: '#fee2e2',
                color: '#dc2626',
                padding: '3px 8px',
                borderRadius: '6px',
              }}
            >
              ADMIN ONLY
            </span>
          </div>
          <p style={{ margin: 0, fontSize: '14px', color: 'var(--text-muted)' }}>
            Quản lý tập trung toàn bộ danh sách tài khoản, khóa / khôi phục truy cập và thay đổi vai trò phân quyền hệ thống.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              setIsCreating(true);
              setEditingUser(null);
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            style={{
              padding: '10px 18px',
              backgroundColor: 'var(--color-primary)',
              color: '#ffffff',
              border: 'none',
              borderRadius: 'var(--radius-sm)',
              fontSize: '13px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: '0 2px 6px rgba(26,115,232,0.25)',
            }}
          >
            <UserPlus size={15} strokeWidth={2.5} />
            <span>Thêm tài khoản mới</span>
          </button>
          <button
            onClick={loadUsers}
            style={{
              padding: '10px 16px',
              backgroundColor: 'var(--surface-muted)',
              color: 'var(--text-secondary)',
              border: '1px solid var(--border-input)',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
            title="Tải lại danh sách người dùng"
          >
            <RefreshCw size={14} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '16px' }}>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '18px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Users size={14} />
            <span>TỔNG NGƯỜI DÙNG</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: 'var(--text-primary)', marginTop: '4px' }}>{stats.total}</div>
        </div>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '18px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: '#2563eb', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <GraduationCap size={14} />
            <span>GIÁO VIÊN</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#2563eb', marginTop: '4px' }}>{stats.teachers}</div>
        </div>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '18px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <BookOpen size={14} />
            <span>HỌC SINH</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#16a34a', marginTop: '4px' }}>{stats.students}</div>
        </div>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '18px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: '#9333ea', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ShieldCheck size={14} />
            <span>THẨM ĐỊNH</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#9333ea', marginTop: '4px' }}>{stats.reviewers}</div>
        </div>
        <div style={{ backgroundColor: 'var(--bg-card)', padding: '18px', borderRadius: '12px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ fontSize: '12px', color: '#dc2626', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <UserX size={14} />
            <span>ĐÃ VÔ HIỆU HÓA</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#dc2626', marginTop: '4px' }}>{stats.disabled}</div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div
        style={{
          backgroundColor: 'var(--bg-card)',
          padding: '18px 24px',
          borderRadius: '14px',
          border: '1px solid var(--border-subtle)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '16px',
        }}
      >
        {/* Search Input */}
        <div style={{ flex: 1, position: 'relative' }}>
          <Search size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
          <input
            type="text"
            placeholder="Tìm kiếm theo Họ tên, Email, Trường học..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 14px 10px 36px',
              borderRadius: '8px',
              border: '1px solid var(--border-input)',
              fontSize: '14px',
              outline: 'none',
            }}
          />
        </div>

        {/* Filter Role */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 600 }}>Vai trò:</span>
          <select
            value={selectedRoleFilter}
            onChange={(e) => setSelectedRoleFilter(e.target.value)}
            style={{
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid var(--border-input)',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: 'var(--surface-muted)',
              cursor: 'pointer',
              outline: 'none',
            }}
          >
            <option value="ALL">Tất cả vai trò</option>
            <option value="TEACHER">Giáo viên (TEACHER)</option>
            <option value="STUDENT">Học sinh (STUDENT)</option>
            <option value="REVIEWER">Thẩm định (REVIEWER)</option>
            <option value="ADMIN">Quản trị (ADMIN)</option>
          </select>
        </div>

        {/* Filter Status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 600 }}>Trạng thái:</span>
          <select
            value={selectedStatusFilter}
            onChange={(e) => setSelectedStatusFilter(e.target.value)}
            style={{
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid var(--border-input)',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: 'var(--surface-muted)',
              cursor: 'pointer',
              outline: 'none',
            }}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động (active)</option>
            <option value="disabled">Đã khóa (disabled)</option>
          </select>
        </div>
      </div>

      {/* NATIVE CARD: Chỉnh sửa thông tin tài khoản (Section 6.3) */}
      {editingUser && (
        <div className="card ops-form" style={{ borderColor: 'var(--color-primary)', boxShadow: 'var(--shadow-md)', animation: 'fadeIn 0.25s ease' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
            <div>
              <div className="eyebrow">Hồ sơ người dùng • Quản trị viên (RBAC)</div>
              <h2 style={{ margin: '0.25rem 0 0.5rem', fontSize: '1.25rem', fontWeight: 800 }}>
                Chỉnh sửa thông tin tài khoản: {editingUser.full_name}
              </h2>
              <div style={{ fontSize: '13px', color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <span>Mã ID: <code>{editingUser.id}</code></span>
                <span>•</span>
                <span className={`status ${editingUser.status === 'active' ? '' : 'bad'}`}>
                  {editingUser.status === 'active' ? '● Đang hoạt động' : '● Đã vô hiệu hóa'}
                </span>
                <span>•</span>
                <span>Tạo ngày: {new Date(editingUser.created_at).toLocaleDateString('vi-VN')}</span>
              </div>
            </div>
            <button type="button" className="secondary" onClick={handleCancelEdit} style={{ fontSize: '13px', padding: '6px 14px' }}>
              ✕ Đóng form
            </button>
          </div>

          <form onSubmit={handleSaveEdit} style={{ marginTop: '20px' }}>
            <div className="ops-form-grid">
              <div className="ops-field">
                <label>Họ và tên *</label>
                <input
                  type="text"
                  value={editFullName}
                  onChange={(e) => setEditFullName(e.target.value)}
                  required
                  placeholder="Nhập họ và tên..."
                />
              </div>

              <div className="ops-field">
                <label>Email đăng nhập (Cố định theo hệ thống)</label>
                <input
                  type="email"
                  value={editingUser.email}
                  disabled
                  style={{ backgroundColor: 'var(--color-surface-variant)', color: 'var(--color-text-secondary)', cursor: 'not-allowed' }}
                />
              </div>

              <div className="ops-field">
                <label>Vai trò phân quyền hệ thống (Role)</label>
                <select
                  value={editRole}
                  onChange={(e) => setEditRole(e.target.value as RoleType)}
                >
                  <option value="TEACHER">TEACHER (Giáo viên)</option>
                  <option value="STUDENT">STUDENT (Học sinh)</option>
                  <option value="REVIEWER">REVIEWER (Thẩm định viên)</option>
                  <option value="ADMIN">ADMIN (Quản trị viên)</option>
                </select>
              </div>

              <div className="ops-field">
                <label>Trường học / Đơn vị công tác</label>
                <input
                  type="text"
                  value={editSchool}
                  onChange={(e) => setEditSchool(e.target.value)}
                  placeholder="Ví dụ: THPT Chuyên Khoa Học Tự Nhiên"
                />
              </div>
            </div>

            <div className="ops-actions" style={{ marginTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                {editingUser.status === 'disabled' ? (
                  <button
                    type="button"
                    onClick={() => handleRestoreUser(editingUser)}
                    style={{ backgroundColor: '#10b981', color: '#ffffff', border: 'none', borderRadius: 'var(--radius-sm)', padding: '10px 18px', fontWeight: 700, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <RotateCcw size={14} />
                    <span>Khôi phục tài khoản (Restore)</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    className="danger"
                    onClick={() => handleDisableUser(editingUser)}
                    disabled={editingUser.id === currentUser?.id}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <UserX size={14} />
                    <span>Khóa tài khoản (Disable)</span>
                  </button>
                )}
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button type="button" className="secondary" onClick={handleCancelEdit}>
                  Hủy thao tác
                </button>
                <button type="submit">
                  Lưu cập nhật tài khoản
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* NATIVE CARD: Thêm tài khoản mới (Section 6.3) */}
      {isCreating && (
        <div className="card ops-form" style={{ borderColor: 'var(--color-primary)', boxShadow: 'var(--shadow-md)', animation: 'fadeIn 0.25s ease' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div className="eyebrow">Tài khoản mới • Quản trị viên (RBAC)</div>
              <h2 style={{ margin: '0.25rem 0 0.5rem', fontSize: '1.25rem', fontWeight: 800 }}>
                Thêm tài khoản người dùng mới
              </h2>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-text-secondary)' }}>
                Khởi tạo tài khoản trực tiếp trên hệ thống PhysLive với phân quyền chuẩn.
              </p>
            </div>
            <button type="button" className="secondary" onClick={() => setIsCreating(false)} style={{ fontSize: '13px', padding: '6px 14px' }}>
              ✕ Đóng form
            </button>
          </div>

          <form onSubmit={handleCreateUser} style={{ marginTop: '20px' }}>
            <div className="ops-form-grid">
              <div className="ops-field">
                <label>Họ và tên *</label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Thầy Trần Quốc Anh"
                  value={newFullName}
                  onChange={(e) => setNewFullName(e.target.value)}
                />
              </div>

              <div className="ops-field">
                <label>Email đăng nhập *</label>
                <input
                  type="email"
                  required
                  placeholder="quocanh.teacher@physlive.edu.vn"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                />
              </div>

              <div className="ops-field">
                <label>Vai trò phân quyền (Role) *</label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value as RoleType)}
                >
                  <option value="TEACHER">TEACHER (Giáo viên)</option>
                  <option value="STUDENT">STUDENT (Học sinh)</option>
                  <option value="REVIEWER">REVIEWER (Thẩm định viên)</option>
                  <option value="ADMIN">ADMIN (Quản trị viên)</option>
                </select>
              </div>

              <div className="ops-field">
                <label>Trường học / Đơn vị</label>
                <input
                  type="text"
                  placeholder="THPT Chuyên Hà Nội - Amsterdam"
                  value={newSchool}
                  onChange={(e) => setNewSchool(e.target.value)}
                />
              </div>
            </div>

            <div className="ops-actions" style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button type="button" className="secondary" onClick={() => setIsCreating(false)}>
                Hủy bỏ
              </button>
              <button type="submit">
                Lưu & Khởi tạo tài khoản
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Users Table */}
      <div
        style={{
          backgroundColor: 'var(--bg-card)',
          borderRadius: '16px',
          border: '1px solid var(--border-subtle)',
          overflow: 'hidden',
          boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ backgroundColor: 'var(--surface-muted)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-secondary)', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              <th style={{ padding: '14px 20px', fontWeight: 700 }}>Người dùng</th>
              <th style={{ padding: '14px 16px', fontWeight: 700 }}>Trường / Đơn vị</th>
              <th style={{ padding: '14px 16px', fontWeight: 700 }}>Vai trò (RBAC)</th>
              <th style={{ padding: '14px 16px', fontWeight: 700 }}>Trạng thái</th>
              <th style={{ padding: '14px 16px', fontWeight: 700 }}>Ngày tham gia</th>
              <th style={{ padding: '14px 20px', fontWeight: 700, textAlign: 'right' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải danh sách người dùng...
                </td>
              </tr>
            ) : filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Không tìm thấy tài khoản nào phù hợp với bộ lọc hiện tại.
                </td>
              </tr>
            ) : (
              filteredUsers.map((u) => {
                const isDisabled = u.status === 'disabled';
                return (
                  <tr
                    key={u.id}
                    style={{
                      borderBottom: '1px solid var(--border-subtle)',
                      backgroundColor: isDisabled ? 'var(--status-warn-bg)' : 'var(--bg-card)',
                      transition: 'background-color 0.15s ease',
                    }}
                  >
                    {/* User Info */}
                    <td style={{ padding: '14px 20px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div
                          style={{
                            width: '38px',
                            height: '38px',
                            borderRadius: '50%',
                            backgroundColor: isDisabled ? '#94a3b8' : '#2563eb',
                            color: '#ffffff',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 700,
                            fontSize: '15px',
                            flexShrink: 0,
                          }}
                        >
                          {u.full_name.charAt(0)}
                        </div>
                        <div>
                          <div style={{ fontWeight: 700, color: isDisabled ? 'var(--text-muted)' : 'var(--text-primary)', fontSize: '14px' }}>
                            {u.full_name}
                            {u.id === currentUser?.id && (
                              <span style={{ marginLeft: '6px', fontSize: '11px', color: '#2563eb', fontWeight: 600 }}>
                                (Bạn)
                              </span>
                            )}
                          </div>
                          <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{u.email}</div>
                        </div>
                      </div>
                    </td>

                    {/* School */}
                    <td style={{ padding: '14px 16px', fontSize: '13px', color: 'var(--text-secondary)' }}>
                      {u.school_name || '—'}
                    </td>

                    {/* Role & Role Switcher */}
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <RoleBadge role={u.role} />
                        <select
                          value={u.role}
                          onChange={(e) => handleChangeRole(u, e.target.value as RoleType)}
                          title="Đổi vai trò phân quyền người dùng này"
                          style={{
                            padding: '4px 8px',
                            borderRadius: '6px',
                            border: '1px solid var(--border-input)',
                            fontSize: '12px',
                            fontWeight: 600,
                            backgroundColor: 'var(--bg-card)',
                            cursor: 'pointer',
                            outline: 'none',
                          }}
                        >
                          <option value="TEACHER">TEACHER</option>
                          <option value="STUDENT">STUDENT</option>
                          <option value="REVIEWER">REVIEWER</option>
                          <option value="ADMIN">ADMIN</option>
                        </select>
                      </div>
                    </td>

                    {/* Status */}
                    <td style={{ padding: '14px 16px' }}>
                      {isDisabled ? (
                        <span
                          style={{
                            padding: '4px 10px',
                            borderRadius: '9999px',
                            backgroundColor: '#fee2e2',
                            color: '#dc2626',
                            fontSize: '12px',
                            fontWeight: 700,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                          }}
                        >
                          ● Đã khóa
                        </span>
                      ) : (
                        <span
                          style={{
                            padding: '4px 10px',
                            borderRadius: '9999px',
                            backgroundColor: '#dcfce7',
                            color: '#15803d',
                            fontSize: '12px',
                            fontWeight: 700,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                          }}
                        >
                          ● Hoạt động
                        </span>
                      )}
                    </td>

                    {/* Date */}
                    <td style={{ padding: '14px 16px', fontSize: '12px', color: 'var(--text-muted)' }}>
                      {u.created_at ? new Date(u.created_at).toLocaleDateString('vi-VN') : '—'}
                    </td>

                    {/* Actions */}
                    <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                        {/* Edit / View User Button */}
                        <button
                          onClick={() => handleStartEdit(u)}
                          style={{
                            padding: '6px 12px',
                            backgroundColor: '#eef3fe',
                            color: '#1a73e8',
                            border: '1px solid #c2e7ff',
                            borderRadius: '6px',
                            fontSize: '12px',
                            fontWeight: 700,
                            cursor: 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                          }}
                          title="Xem và chỉnh sửa hồ sơ tài khoản"
                        >
                          <Eye size={13} />
                          <span>Sửa & Xem</span>
                        </button>

                        {/* Disable / Restore Button */}
                        {isDisabled ? (
                          <button
                            onClick={() => handleRestoreUser(u)}
                            style={{
                              padding: '6px 12px',
                              backgroundColor: '#ecfdf5',
                              color: '#059669',
                              border: '1px solid #a7f3d0',
                              borderRadius: '6px',
                              fontSize: '12px',
                              fontWeight: 700,
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                            }}
                            title="Khôi phục tài khoản hoạt động bình thường (Restore User)"
                          >
                            <RotateCcw size={13} />
                            <span>Mở khóa</span>
                          </button>
                        ) : (
                          <button
                            onClick={() => handleDisableUser(u)}
                            disabled={u.id === currentUser?.id}
                            style={{
                              padding: '6px 12px',
                              backgroundColor: u.id === currentUser?.id ? '#f1f5f9' : '#fef2f2',
                              color: u.id === currentUser?.id ? '#94a3b8' : '#dc2626',
                              border: u.id === currentUser?.id ? '1px solid #e2e8f0' : '1px solid #fecaca',
                              borderRadius: '6px',
                              fontSize: '12px',
                              fontWeight: 700,
                              cursor: u.id === currentUser?.id ? 'not-allowed' : 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                            }}
                            title={u.id === currentUser?.id ? 'Không thể tự khóa tài khoản của mình' : 'Vô hiệu hóa tài khoản (Disable User)'}
                          >
                            <UserX size={13} />
                            <span>Khóa</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Admin;
