import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Shield, Key, CheckCircle, AlertCircle, X, LogOut, Info } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { currentUser, updateProfile, logout } = useAuth();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [schoolName, setSchoolName] = useState('');
  
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');
  
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.full_name);
      setSchoolName(currentUser.school_name || 'THPT Chuyên Khoa Học Tự Nhiên');
    }
  }, [currentUser]);

  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');
    if (!fullName) {
      setError('Họ và tên không được để trống.');
      return;
    }
    const success = updateProfile(fullName, schoolName);
    if (success) {
      setSuccessMessage('Hồ sơ đã được cập nhật thành công!');
      setTimeout(() => setSuccessMessage(''), 3000);
    } else {
      setError('Đã có lỗi xảy ra khi cập nhật hồ sơ.');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!currentUser) return null;

  return (
    <div style={{ padding: '32px 20px', minHeight: 'calc(100vh - 70px)' }}>
      <div style={{ marginBottom: '24px', textAlign: 'center' }}>
        <h1 style={{ fontSize: '24px', fontWeight: 800, margin: '0 0 8px', color: 'var(--text-primary)' }}>
          Tài khoản của tôi
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px', margin: 0 }}>
          Quản lý thông tin cá nhân và bảo mật
        </p>
      </div>

        <div className="profile-modular-grid">
          {/* Cột trái: Avatar và Tóm tắt */}
          <div>
            <div className="profile-block" style={{ textAlign: 'center', padding: '32px 24px' }}>
              <div style={{ 
                width: '90px', 
                height: '90px', 
                borderRadius: '50%', 
                background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '32px',
                fontWeight: 800,
                margin: '0 auto 16px',
                boxShadow: '0 8px 16px rgba(124, 58, 237, 0.2)'
              }}>
                {currentUser.full_name.charAt(0)}
              </div>
              <h2 style={{ margin: '0 0 4px', fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)' }}>
                {currentUser.full_name}
              </h2>
              <p style={{ margin: '0 0 16px', fontSize: '13px', color: 'var(--text-secondary)' }}>
                {currentUser.email}
              </p>
              
              <div style={{ 
                display: 'inline-flex',
                alignItems: 'center',
                padding: '6px 12px',
                background: 'rgba(59, 130, 246, 0.1)',
                color: '#3b82f6',
                borderRadius: '999px',
                fontSize: '12px',
                fontWeight: 700
              }}>
                Vai trò: {currentUser.role}
              </div>

              <div style={{ marginTop: '24px' }}>
                <button 
                  onClick={handleLogout}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    width: '100%',
                    padding: '10px',
                    background: '#fef2f2',
                    color: '#ef4444',
                    border: '1px solid #fee2e2',
                    borderRadius: '10px',
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                >
                  <LogOut size={16} /> Đăng xuất
                </button>
              </div>
            </div>
          </div>

          {/* Cột phải: Chi tiết */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            {/* Block 1: Thông tin cá nhân */}
            <div className="profile-block">
              <div className="profile-block-header">
                <Info size={20} color="#8b5cf6" />
                <h3>Thông tin cá nhân</h3>
              </div>
              <div className="profile-block-content">
                {successMessage && (
                  <div style={{ 
                    display: 'flex', alignItems: 'center', gap: '8px', padding: '12px', 
                    background: '#ecfdf5', border: '1px solid #d1fae5', color: '#059669', 
                    borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: 600
                  }}>
                    <CheckCircle size={18} /> {successMessage}
                  </div>
                )}
                {error && (
                  <div style={{ 
                    display: 'flex', alignItems: 'center', gap: '8px', padding: '12px', 
                    background: '#fef2f2', border: '1px solid #fee2e2', color: '#ef4444', 
                    borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: 600
                  }}>
                    <AlertCircle size={18} /> {error}
                  </div>
                )}

                <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div className="field" style={{ margin: 0 }}>
                    <label>Họ và tên</label>
                    <input 
                      type="text" 
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                    />
                  </div>
                  <div className="field" style={{ margin: 0 }}>
                    <label>Trường học</label>
                    <input 
                      type="text" 
                      value={schoolName}
                      onChange={(e) => setSchoolName(e.target.value)}
                    />
                  </div>
                  <div>
                    <button type="submit" className="btn-primary" style={{ padding: '0 24px' }}>
                      Lưu thay đổi
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* Block 2: Bảo mật */}
            <div className="profile-block">
              <div className="profile-block-header">
                <Shield size={20} color="#10b981" />
                <h3>Bảo mật & Tài khoản</h3>
              </div>
              <div className="profile-block-content">
                <div className="security-item">
                  <div className="security-item-info">
                    <div className="security-item-icon">
                      <Key size={20} />
                    </div>
                    <div className="security-item-text">
                      <h4>Mật khẩu</h4>
                      <p>Bảo vệ tài khoản của bạn</p>
                    </div>
                  </div>
                  <button className="secondary" onClick={() => setShowPasswordModal(true)}>
                    Thay đổi
                  </button>
                </div>

                <div className="profile-stats-grid">
                  <div className="profile-stat-box">
                    <div className="label">Ngày gia nhập</div>
                    <div className="value">15/09/2026</div>
                  </div>
                  <div className="profile-stat-box">
                    <div className="label">ID Người dùng</div>
                    <div className="value">{currentUser.id}</div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>

      {showPasswordModal && (
        <ChangePasswordModal onClose={() => setShowPasswordModal(false)} />
      )}
    </div>
  );
};

// Modal Đổi Mật Khẩu
const ChangePasswordModal: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPassword !== confirmPassword) {
      setError('Mật khẩu mới không khớp!');
      return;
    }
    if (newPassword.length < 6) {
      setError('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }
    
    // Giả lập call API
    setSuccess('Đổi mật khẩu thành công!');
    setTimeout(() => {
      onClose();
    }, 1500);
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content">
        <div className="modal-header">
          <h3><Key size={20} /> Đổi mật khẩu</h3>
          <p>Cập nhật mật khẩu an toàn</p>
          <button className="modal-close-btn" onClick={onClose}><X size={18} /></button>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {error && (
              <div style={{ padding: '10px', background: '#fef2f2', color: '#ef4444', borderRadius: '8px', fontSize: '13px', fontWeight: 600 }}>
                {error}
              </div>
            )}
            {success && (
              <div style={{ padding: '10px', background: '#ecfdf5', color: '#059669', borderRadius: '8px', fontSize: '13px', fontWeight: 600 }}>
                {success}
              </div>
            )}

            <div className="field" style={{ margin: 0 }}>
              <label>Mật khẩu hiện tại</label>
              <input 
                type="password" 
                value={oldPassword} 
                onChange={(e) => setOldPassword(e.target.value)} 
                required 
                placeholder="Nhập mật khẩu hiện tại"
              />
            </div>
            
            <div className="field" style={{ margin: 0 }}>
              <label>Mật khẩu mới</label>
              <input 
                type="password" 
                value={newPassword} 
                onChange={(e) => setNewPassword(e.target.value)} 
                required 
                placeholder="Nhập mật khẩu mới (ít nhất 6 ký tự)"
              />
            </div>
            
            <div className="field" style={{ margin: 0 }}>
              <label>Xác nhận mật khẩu mới</label>
              <input 
                type="password" 
                value={confirmPassword} 
                onChange={(e) => setConfirmPassword(e.target.value)} 
                required 
                placeholder="Nhập lại mật khẩu mới"
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#b45309', background: '#fef3c7', padding: '8px 12px', borderRadius: '8px' }}>
              <Shield size={14} /> Bảo mật: Mật khẩu của bạn được mã hóa tự động
            </div>
          </div>
          
          <div className="modal-footer">
            <button type="button" className="secondary" onClick={onClose}>Hủy</button>
            <button type="submit" className="btn-primary">Đổi mật khẩu</button>
          </div>
        </form>
      </div>
    </div>
  );
};
