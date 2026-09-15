import React, { useState, useEffect } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { RoleBadge } from '../../components/common/RoleBadge';
import { UserCheck, Edit3, KeyRound, LogOut, ArrowLeft, CheckCircle2 } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { currentUser, setUser, logout } = useAuth();
  const navigate = useNavigate();

  // Mode: 'view' | 'edit' | 'password' (Chuyển native sang thẻ khác theo yêu cầu Leader)
  const [mode, setMode] = useState<'view' | 'edit' | 'password'>('view');

  // Edit fields
  const [fullName, setFullName] = useState(currentUser?.full_name || '');
  const [schoolName, setSchoolName] = useState(currentUser?.school_name || 'THPT Chuyên Khoa Học Tự Nhiên');
  const [newPasswordOptional, setNewPasswordOptional] = useState('');

  // Dedicated Password change fields
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [notification, setNotification] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.full_name);
      setSchoolName(currentUser.school_name || 'THPT Chuyên Khoa Học Tự Nhiên');
    }
  }, [currentUser]);

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  const showToast = (msg: string, isError = false) => {
    if (isError) {
      setErrorMessage(msg);
      setTimeout(() => setErrorMessage(null), 3500);
    } else {
      setNotification(msg);
      setTimeout(() => setNotification(null), 3500);
    }
  };

  // Lưu thông tin từ thẻ Edit (Mục 6.3)
  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      showToast('Họ và tên không được để trống!', true);
      return;
    }

    setUser({
      ...currentUser,
      full_name: fullName.trim(),
      school_name: schoolName,
      updated_at: new Date().toISOString(),
    });

    setMode('view');
    showToast('Đã lưu thông tin tài khoản thành công!');
  };

  // Đổi mật khẩu
  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      showToast('Vui lòng điền đầy đủ các trường mật khẩu!', true);
      return;
    }
    if (newPassword.length < 6) {
      showToast('Mật khẩu mới phải có ít nhất 6 ký tự!', true);
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast('Xác nhận mật khẩu mới không khớp!', true);
      return;
    }

    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setMode('view');
    showToast('Đổi mật khẩu thành công!');
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{ maxWidth: '820px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header gọn gàng, chuẩn typography */}
      <div>
        <span className="eyebrow">Hồ sơ cá nhân & Định danh</span>
        <h1 style={{ margin: '4px 0 6px', fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>
          Tài khoản của bạn
        </h1>
        <p style={{ margin: 0, fontSize: '13.5px', color: 'var(--text-secondary)' }}>
          Quản lý thông tin định danh và phân quyền trong hệ thống PhysLive Studio.
        </p>
      </div>

      {/* Thông báo trạng thái */}
      {notification && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 16px', background: '#e6f4ea', border: '1px solid #ceead6', borderRadius: '10px', color: '#137333', fontSize: '13.5px', fontWeight: 600 }}>
          <CheckCircle2 size={18} color="#137333" />
          <span>{notification}</span>
        </div>
      )}

      {errorMessage && (
        <div className="ambiguity" style={{ background: '#fce8e6', borderColor: '#fad2cf', color: '#c5221f', margin: 0 }}>
          ⚠️ {errorMessage}
        </div>
      )}

      {/* ==================================================================== */}
      {/* THẺ 1: XEM THÔNG TIN TỔNG QUAN (VIEW CARD MODE)                      */}
      {/* ==================================================================== */}
      {mode === 'view' && (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Top User Hero */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px', paddingBottom: '20px', borderBottom: '1px solid var(--border-card)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div
                style={{
                  width: '58px',
                  height: '58px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #1a73e8, #174ea6)',
                  color: '#ffffff',
                  display: 'grid',
                  placeItems: 'center',
                  fontWeight: 800,
                  fontSize: '22px',
                  boxShadow: '0 3px 10px rgba(26, 115, 232, 0.25)',
                }}
              >
                {currentUser.full_name.charAt(0).toUpperCase()}
              </div>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                  <h2 style={{ margin: 0, fontSize: '19px', fontWeight: 800, color: 'var(--text-primary)' }}>
                    {currentUser.full_name}
                  </h2>
                  <RoleBadge role={currentUser.role} />
                </div>
                <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  {currentUser.email}
                </div>
              </div>
            </div>

            <span className="status">
              Hoạt động
            </span>
          </div>

          {/* Clean 2-Column Info Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '18px 24px' }}>
            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Họ và tên
              </div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-primary)' }}>
                {currentUser.full_name}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Địa chỉ Email
              </div>
              <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--text-primary)' }}>
                {currentUser.email}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Vai trò hệ thống
              </div>
              <div style={{ fontSize: '14px', fontWeight: 700, color: '#1a73e8' }}>
                {currentUser.role}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Trường học / Đơn vị liên kết
              </div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>
                {currentUser.school_name || 'THPT Chuyên Khoa Học Tự Nhiên'}
              </div>
            </div>
          </div>

          {/* Action Buttons: Chuyển Native sang thẻ Edit */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px', paddingTop: '18px', borderTop: '1px solid var(--border-card)' }}>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                className="btn-primary"
                onClick={() => setMode('edit')}
              >
                <Edit3 size={15} />
                <span>Chỉnh sửa thông tin</span>
              </button>

              <button
                type="button"
                className="secondary"
                onClick={() => setMode('password')}
              >
                <KeyRound size={15} />
                <span>Đổi mật khẩu</span>
              </button>
            </div>

            <button
              type="button"
              className="danger"
              onClick={handleLogout}
            >
              <LogOut size={14} />
              <span>Đăng xuất</span>
            </button>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* THẺ 2: NATIVE FORM CHỈNH SỬA THÔNG TIN 2 CỘT (MỤC 6.3 UI_DESIGN_GUIDE)*/}
      {/* ==================================================================== */}
      {mode === 'edit' && (
        <form className="ops-form" onSubmit={handleSaveProfile}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h3 style={{ margin: 0 }}>Chỉnh sửa thông tin · {currentUser.email}</h3>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Mục 6.3 Design System</span>
          </div>

          <div className="ops-form-grid">
            <label className="ops-field">
              Họ và tên
              <input
                name="fullName"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                maxLength={200}
                placeholder="Nhập họ và tên đầy đủ"
              />
            </label>

            <label className="ops-field">
              Vai trò (Role)
              <select name="role" defaultValue={currentUser.role} disabled style={{ backgroundColor: '#f1f3f4', cursor: 'not-allowed' }}>
                <option value="TEACHER">TEACHER (Giáo viên)</option>
                <option value="STUDENT">STUDENT (Học sinh)</option>
                <option value="REVIEWER">REVIEWER (Kiểm duyệt viên)</option>
                <option value="ADMIN">ADMIN (Quản trị viên)</option>
              </select>
            </label>

            <label className="ops-field">
              Trường học / Đơn vị liên kết
              <select
                name="schoolName"
                value={schoolName}
                onChange={(e) => setSchoolName(e.target.value)}
              >
                <option value="THPT Chuyên Khoa Học Tự Nhiên">THPT Chuyên Khoa Học Tự Nhiên</option>
                <option value="THPT Chuyên Hà Nội - Amsterdam">THPT Chuyên Hà Nội - Amsterdam</option>
                <option value="THPT FPT Hà Nội">THPT FPT Hà Nội</option>
                <option value="THPT FPT Cần Thơ">THPT FPT Cần Thơ</option>
                <option value="THPT Chuyên Lê Hồng Phong TP.HCM">THPT Chuyên Lê Hồng Phong TP.HCM</option>
              </select>
            </label>

            <label className="ops-field">
              Mật khẩu mới (Nếu cần đổi)
              <input
                type="password"
                name="password"
                minLength={6}
                placeholder="Để trống nếu giữ nguyên"
                value={newPasswordOptional}
                onChange={(e) => setNewPasswordOptional(e.target.value)}
              />
            </label>
          </div>

          <div className="ops-actions">
            <button type="submit" className="btn-primary">
              <UserCheck size={16} />
              <span>Lưu tài khoản</span>
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setFullName(currentUser.full_name);
                setSchoolName(currentUser.school_name || 'THPT Chuyên Khoa Học Tự Nhiên');
                setNewPasswordOptional('');
                setMode('view');
              }}
            >
              <ArrowLeft size={15} />
              <span>Hủy</span>
            </button>
          </div>
        </form>
      )}

      {/* ==================================================================== */}
      {/* THẺ 3: FORM ĐỔI MẬT KHẨU RIÊNG BIỆT (PASSWORD CHANGE CARD)           */}
      {/* ==================================================================== */}
      {mode === 'password' && (
        <form className="ops-form" onSubmit={handleChangePassword}>
          <h3>Đổi mật khẩu bảo vệ tài khoản</h3>
          <p style={{ margin: '0 0 18px', fontSize: '13px', color: 'var(--text-secondary)' }}>
            Mật khẩu mới cần tối thiểu 6 ký tự để đảm bảo an toàn truy cập.
          </p>

          <div style={{ display: 'grid', gap: '14px', maxWidth: '460px' }}>
            <label className="ops-field">
              Mật khẩu hiện tại
              <input
                type="password"
                placeholder="Nhập mật khẩu đang dùng..."
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </label>

            <label className="ops-field">
              Mật khẩu mới
              <input
                type="password"
                placeholder="Nhập mật khẩu mới..."
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={6}
              />
            </label>

            <label className="ops-field">
              Xác nhận mật khẩu mới
              <input
                type="password"
                placeholder="Nhập lại mật khẩu mới..."
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </label>
          </div>

          <div className="ops-actions">
            <button type="submit" className="btn-primary">
              <span>Xác nhận đổi mật khẩu</span>
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setCurrentPassword('');
                setNewPassword('');
                setConfirmPassword('');
                setMode('view');
              }}
            >
              <span>Hủy</span>
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default ProfilePage;
