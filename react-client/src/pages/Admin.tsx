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

  // Modal view detail user
  const [viewingUser, setViewingUser] = useState<User | null>(null);

  // Modal create user
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
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
      if (viewingUser?.id === user.id) {
        setViewingUser({ ...viewingUser, status: 'disabled' });
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
      if (viewingUser?.id === user.id) {
        setViewingUser({ ...viewingUser, status: 'active' });
      }
      showToast(`Đã khôi phục hoạt động cho tài khoản: ${user.full_name}`, 'success');
    } catch {
      showToast('Lỗi khi khôi phục người dùng', 'danger');
    }
  };

  // 3. Đổi vai trò (Change Role)
  const handleChangeRole = async (user: User, nextRole: RoleType) => {
    if (user.role === nextRole) return;
    if (user.id === currentUser?.id && nextRole !== 'ADMIN') {
      const confirmSelf = window.confirm('Cảnh báo: Bạn đang hạ quyền tài khoản Admin của chính mình! Bạn có chắc không?');
      if (!confirmSelf) return;
    }

    try {
      const updatedList = await changeUserRoleApi(user.id, nextRole);
      setUsers(updatedList);
      if (viewingUser?.id === user.id) {
        setViewingUser({ ...viewingUser, role: nextRole, role_id: `r-${nextRole.toLowerCase()}` });
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
      alert('Vui lòng nhập đầy đủ họ tên và email!');
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
    setIsCreateModalOpen(false);
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
          backgroundColor: '#ffffff',
          padding: '24px',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <ShieldAlert size={26} color="#dc2626" />
            <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
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
          <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
            Quản lý tập trung toàn bộ danh sách tài khoản, khóa / khôi phục truy cập và thay đổi vai trò phân quyền hệ thống.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            style={{
              padding: '10px 18px',
              backgroundColor: '#2563eb',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: '0 2px 6px rgba(37,99,235,0.25)',
            }}
          >
            <UserPlus size={15} strokeWidth={2.5} />
            <span>Thêm tài khoản mới</span>
          </button>
          <button
            onClick={loadUsers}
            style={{
              padding: '10px 16px',
              backgroundColor: '#f8fafc',
              color: '#475569',
              border: '1px solid #cbd5e1',
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
        <div style={{ backgroundColor: '#ffffff', padding: '18px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Users size={14} />
            <span>TỔNG NGƯỜI DÙNG</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#0f172a', marginTop: '4px' }}>{stats.total}</div>
        </div>
        <div style={{ backgroundColor: '#ffffff', padding: '18px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '12px', color: '#2563eb', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <GraduationCap size={14} />
            <span>GIÁO VIÊN</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#2563eb', marginTop: '4px' }}>{stats.teachers}</div>
        </div>
        <div style={{ backgroundColor: '#ffffff', padding: '18px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '12px', color: '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <BookOpen size={14} />
            <span>HỌC SINH</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#16a34a', marginTop: '4px' }}>{stats.students}</div>
        </div>
        <div style={{ backgroundColor: '#ffffff', padding: '18px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '12px', color: '#9333ea', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ShieldCheck size={14} />
            <span>THẨM ĐỊNH</span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#9333ea', marginTop: '4px' }}>{stats.reviewers}</div>
        </div>
        <div style={{ backgroundColor: '#ffffff', padding: '18px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
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
          backgroundColor: '#ffffff',
          padding: '18px 24px',
          borderRadius: '14px',
          border: '1px solid #e2e8f0',
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
              border: '1px solid #cbd5e1',
              fontSize: '14px',
              outline: 'none',
            }}
          />
        </div>

        {/* Filter Role */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', color: '#64748b', fontWeight: 600 }}>Vai trò:</span>
          <select
            value={selectedRoleFilter}
            onChange={(e) => setSelectedRoleFilter(e.target.value)}
            style={{
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: '#f8fafc',
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
          <span style={{ fontSize: '13px', color: '#64748b', fontWeight: 600 }}>Trạng thái:</span>
          <select
            value={selectedStatusFilter}
            onChange={(e) => setSelectedStatusFilter(e.target.value)}
            style={{
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: '#f8fafc',
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

      {/* Users Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          overflow: 'hidden',
          boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
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
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
                  Đang tải danh sách người dùng...
                </td>
              </tr>
            ) : filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>
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
                      borderBottom: '1px solid #f1f5f9',
                      backgroundColor: isDisabled ? '#fff7ed' : '#ffffff',
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
                          <div style={{ fontWeight: 700, color: isDisabled ? '#64748b' : '#0f172a', fontSize: '14px' }}>
                            {u.full_name}
                            {u.id === currentUser?.id && (
                              <span style={{ marginLeft: '6px', fontSize: '11px', color: '#2563eb', fontWeight: 600 }}>
                                (Bạn)
                              </span>
                            )}
                          </div>
                          <div style={{ fontSize: '12px', color: '#64748b' }}>{u.email}</div>
                        </div>
                      </div>
                    </td>

                    {/* School */}
                    <td style={{ padding: '14px 16px', fontSize: '13px', color: '#475569' }}>
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
                            border: '1px solid #cbd5e1',
                            fontSize: '12px',
                            fontWeight: 600,
                            backgroundColor: '#ffffff',
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
                    <td style={{ padding: '14px 16px', fontSize: '12px', color: '#64748b' }}>
                      {u.created_at ? new Date(u.created_at).toLocaleDateString('vi-VN') : '—'}
                    </td>

                    {/* Actions */}
                    <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
                        {/* View User Button */}
                        <button
                          onClick={() => setViewingUser(u)}
                          style={{
                            padding: '6px 12px',
                            backgroundColor: '#f1f5f9',
                            color: '#334155',
                            border: '1px solid #cbd5e1',
                            borderRadius: '6px',
                            fontSize: '12px',
                            fontWeight: 600,
                            cursor: 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                          }}
                          title="Xem chi tiết hồ sơ tài khoản (View User)"
                        >
                          <Eye size={13} />
                          <span>Chi tiết</span>
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

      {/* MODAL 1: View User Detail (View User) */}
      {viewingUser && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
          onClick={() => setViewingUser(null)}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '20px',
              maxWidth: '560px',
              width: '100%',
              padding: '28px',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>
                👤 Thông tin chi tiết Người Dùng (View User)
              </h2>
              <button
                onClick={() => setViewingUser(null)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '20px',
                  color: '#94a3b8',
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {/* User Profile Card Header */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                backgroundColor: '#f8fafc',
                padding: '18px',
                borderRadius: '12px',
                border: '1px solid #e2e8f0',
              }}
            >
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #2563eb, #06b6d4)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '22px',
                  fontWeight: 800,
                }}
              >
                {viewingUser.full_name.charAt(0)}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <span style={{ fontSize: '17px', fontWeight: 800, color: '#0f172a' }}>
                    {viewingUser.full_name}
                  </span>
                  <RoleBadge role={viewingUser.role} />
                </div>
                <div style={{ fontSize: '13px', color: '#64748b' }}>{viewingUser.email}</div>
              </div>
            </div>

            {/* Detail Attributes List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '13px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ color: '#64748b', fontWeight: 600 }}>Mã định danh (User ID):</span>
                <span style={{ fontFamily: 'monospace', color: '#0f172a', fontWeight: 700 }}>{viewingUser.id}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ color: '#64748b', fontWeight: 600 }}>Trường học / Đơn vị:</span>
                <span style={{ fontWeight: 600, color: '#0f172a' }}>{viewingUser.school_name || 'Chưa cập nhật'}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ color: '#64748b', fontWeight: 600 }}>Trạng thái tài khoản:</span>
                <span style={{ fontWeight: 700, color: viewingUser.status === 'disabled' ? '#dc2626' : '#16a34a' }}>
                  {viewingUser.status === 'disabled' ? '🚫 Đã vô hiệu hóa (Disabled)' : '✓ Đang hoạt động (Active)'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ color: '#64748b', fontWeight: 600 }}>Ngày khởi tạo tài khoản:</span>
                <span>{new Date(viewingUser.created_at).toLocaleString('vi-VN')}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ color: '#64748b', fontWeight: 600 }}>Cập nhật lần cuối:</span>
                <span>{new Date(viewingUser.updated_at).toLocaleString('vi-VN')}</span>
              </div>
            </div>

            {/* Role Assignment Selector inside modal */}
            <div style={{ backgroundColor: '#f1f5f9', padding: '14px 16px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: '13px', fontWeight: 700, color: '#0f172a' }}>Phân quyền vai trò (Change Role):</div>
                <div style={{ fontSize: '11px', color: '#64748b' }}>Thay đổi quyền truy cập các module theo vai trò</div>
              </div>
              <select
                value={viewingUser.role}
                onChange={(e) => handleChangeRole(viewingUser, e.target.value as RoleType)}
                style={{
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  fontWeight: 700,
                  fontSize: '13px',
                  backgroundColor: '#ffffff',
                  cursor: 'pointer',
                }}
              >
                <option value="TEACHER">TEACHER (Giáo viên)</option>
                <option value="STUDENT">STUDENT (Học sinh)</option>
                <option value="REVIEWER">REVIEWER (Thẩm định)</option>
                <option value="ADMIN">ADMIN (Quản trị viên)</option>
              </select>
            </div>

            {/* Modal Actions */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
              {viewingUser.status === 'disabled' ? (
                <button
                  onClick={() => handleRestoreUser(viewingUser)}
                  style={{
                    padding: '9px 18px',
                    backgroundColor: '#10b981',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  🔄 Khôi phục tài khoản này (Restore)
                </button>
              ) : (
                <button
                  onClick={() => handleDisableUser(viewingUser)}
                  disabled={viewingUser.id === currentUser?.id}
                  style={{
                    padding: '9px 18px',
                    backgroundColor: '#ef4444',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: 700,
                    cursor: viewingUser.id === currentUser?.id ? 'not-allowed' : 'pointer',
                    opacity: viewingUser.id === currentUser?.id ? 0.6 : 1,
                  }}
                >
                  🚫 Khóa tài khoản này (Disable)
                </button>
              )}

              <button
                onClick={() => setViewingUser(null)}
                style={{
                  padding: '9px 18px',
                  backgroundColor: '#f1f5f9',
                  color: '#334155',
                  border: '1px solid #cbd5e1',
                  borderRadius: '8px',
                  fontSize: '13px',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: Create User */}
      {isCreateModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
          onClick={() => setIsCreateModalOpen(false)}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '20px',
              maxWidth: '500px',
              width: '100%',
              padding: '28px',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>
                ➕ Thêm tài khoản người dùng mới
              </h2>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                style={{ background: 'none', border: 'none', fontSize: '20px', color: '#94a3b8', cursor: 'pointer' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateUser} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Họ và tên
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Thầy Trần Quốc Anh"
                  value={newFullName}
                  onChange={(e) => setNewFullName(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Email đăng nhập
                </label>
                <input
                  type="email"
                  required
                  placeholder="quocanh.teacher@physlive.edu.vn"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Vai trò phân quyền (Role)
                </label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value as RoleType)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none', backgroundColor: '#ffffff' }}
                >
                  <option value="TEACHER">Giáo viên (TEACHER)</option>
                  <option value="STUDENT">Học sinh (STUDENT)</option>
                  <option value="REVIEWER">Thẩm định viên (REVIEWER)</option>
                  <option value="ADMIN">Quản trị viên (ADMIN)</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Trường học / Đơn vị
                </label>
                <input
                  type="text"
                  placeholder="THPT Chuyên Hà Nội - Amsterdam"
                  value={newSchool}
                  onChange={(e) => setNewSchool(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  style={{ padding: '10px 16px', backgroundColor: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '8px', fontWeight: 600, cursor: 'pointer' }}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  style={{ padding: '10px 20px', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '8px', fontWeight: 700, cursor: 'pointer' }}
                >
                  Lưu & Khởi tạo tài khoản
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Admin;
