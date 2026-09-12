import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { RoleBadge } from '../../components/common/RoleBadge';

export const ProfilePage: React.FC = () => {
  const { currentUser, setUser, logout } = useAuth();
  const navigate = useNavigate();

  // Profile fields state
  const [fullName, setFullName] = useState(currentUser.full_name);
  const [email] = useState(currentUser.email);
  const [schoolName, setSchoolName] = useState(currentUser.school_name || 'THPT Chuyên Khoa Học Tự Nhiên');
  const [notification, setNotification] = useState<string | null>(null);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdError, setPwdError] = useState('');

  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    setUser({
      ...currentUser,
      full_name: fullName,
      school_name: schoolName,
      updated_at: new Date().toISOString(),
    });
    setNotification('Cập nhật thông tin hồ sơ thành công!');
    setTimeout(() => setNotification(null), 3000);
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    setPwdError('');

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPwdError('Vui lòng điền đầy đủ thông tin đổi mật khẩu!');
      return;
    }

    if (newPassword.length < 6) {
      setPwdError('Mật khẩu mới phải có ít nhất 6 ký tự!');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPwdError('Mật khẩu xác nhận không khớp!');
      return;
    }

    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setNotification('Đổi mật khẩu thành công!');
    setTimeout(() => setNotification(null), 3000);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '900px', margin: '0 auto' }}>
      {/* Header */}
      <div>
        <h1 style={{ margin: '0 0 6px', fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
          Hồ sơ cá nhân & Cài đặt tài khoản (Task H02)
        </h1>
        <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
          Quản lý thông tin định danh, trường học và bảo mật tài khoản PhysLive Studio của bạn.
        </p>
      </div>

      {notification && (
        <div style={{ padding: '12px 18px', backgroundColor: '#ecfdf5', color: '#065f46', border: '1px solid #a7f3d0', borderRadius: '10px', fontSize: '14px', fontWeight: 600 }}>
          ✓ {notification}
        </div>
      )}

      {/* User Hero Banner */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '24px',
          border: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #2563eb, #06b6d4)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '26px',
              boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
            }}
          >
            {currentUser.full_name.charAt(0)}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 800, color: '#0f172a' }}>
                {currentUser.full_name}
              </h2>
              <RoleBadge role={currentUser.role} />
            </div>
            <div style={{ fontSize: '13px', color: '#64748b' }}>
              {currentUser.email} • {currentUser.school_name || 'THPT Chuyên KHTN'}
            </div>
          </div>
        </div>

        <button
          onClick={handleLogout}
          style={{
            padding: '8px 16px',
            backgroundColor: '#fee2e2',
            color: '#dc2626',
            border: '1px solid #fca5a5',
            borderRadius: '8px',
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          Đăng xuất tài khoản ➔
        </button>
      </div>

      {/* 2-Column Section: Personal Info vs Change Password */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
        {/* Left Column: Personal Information Form */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '24px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
            👤 Thông tin tài khoản
          </h3>

          <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Họ và tên
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Địa chỉ Email (Định danh đăng nhập)
              </label>
              <input
                type="email"
                disabled
                value={email}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #e2e8f0', backgroundColor: '#f8fafc', color: '#64748b', fontSize: '14px', cursor: 'not-allowed' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Trường học / Đơn vị công tác
              </label>
              <input
                type="text"
                value={schoolName}
                onChange={(e) => setSchoolName(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Vai trò phân quyền hệ thống (RBAC)
              </label>
              <input
                type="text"
                disabled
                value={currentUser.role}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #e2e8f0', backgroundColor: '#f8fafc', color: '#2563eb', fontWeight: 700, fontSize: '14px', cursor: 'not-allowed' }}
              />
            </div>

            <button
              type="submit"
              style={{
                marginTop: '8px',
                padding: '11px',
                backgroundColor: '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: '8px',
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Lưu thay đổi thông tin
            </button>
          </form>
        </div>

        {/* Right Column: Password Change Form */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '24px', border: '1px solid #e2e8f0' }}>
          <h3 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
            🔒 Đổi mật khẩu
          </h3>

          {pwdError && (
            <div style={{ padding: '10px', backgroundColor: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', fontSize: '12px', marginBottom: '14px' }}>
              ⚠️ {pwdError}
            </div>
          )}

          <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Mật khẩu hiện tại
              </label>
              <input
                type="password"
                placeholder="Nhập mật khẩu cũ..."
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Mật khẩu mới
              </label>
              <input
                type="password"
                placeholder="Tối thiểu 6 ký tự..."
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Xác nhận mật khẩu mới
              </label>
              <input
                type="password"
                placeholder="Nhập lại mật khẩu mới..."
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
              />
            </div>

            <button
              type="submit"
              style={{
                marginTop: '8px',
                padding: '11px',
                backgroundColor: '#0f172a',
                color: '#ffffff',
                border: 'none',
                borderRadius: '8px',
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Cập nhật mật khẩu
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
