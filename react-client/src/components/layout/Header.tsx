import React from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { RoleBadge } from '../common/RoleBadge';
import {
  Atom,
  FlaskConical,
  FileEdit,
  Library as LibraryIcon,
  Target,
  Settings,
  LogOut,
  LogIn,
  UserPlus,
  Home as HomeIcon,
  LayoutDashboard,
} from 'lucide-react';
import { SvgIcon } from '../common/SvgIcon';

export const Header: React.FC = () => {
  const { currentUser, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const [isDarkMode, setIsDarkMode] = React.useState<boolean>(() => {
    try {
      const saved = localStorage.getItem('physlive.theme');
      if (saved) {
        document.documentElement.dataset.theme = saved;
        return saved === 'dark';
      }
    } catch {
      // ignore
    }
    return document.documentElement.dataset.theme === 'dark';
  });

  React.useEffect(() => {
    document.documentElement.dataset.theme = isDarkMode ? 'dark' : 'light';
  }, [isDarkMode]);

  const toggleTheme = () => {
    const next = !isDarkMode;
    setIsDarkMode(next);
    document.documentElement.dataset.theme = next ? 'dark' : 'light';
    try {
      localStorage.setItem('physlive.theme', next ? 'dark' : 'light');
    } catch {
      // ignore
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header
      className="studio-header glass-panel"
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 50,
        backdropFilter: 'blur(16px)',
        borderBottom: '1px solid var(--border-subtle)',
      }}
    >
      {/* 1. Left: Brand Logo */}
      <div className="studio-header-left">
        <Link to="/" className="studio-brand" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none' }}>
          <div
            className="studio-brand-badge"
            style={{
              width: '38px',
              height: '38px',
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #2563eb, #06b6d4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff',
              boxShadow: '0 4px 14px rgba(37, 99, 235, 0.35)',
            }}
          >
            <Atom size={22} className="spin-slow" strokeWidth={2.2} />
          </div>
          <div>
            <div className="studio-brand-name" style={{ fontSize: '17px', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
              PhysLive <span style={{ color: '#2563eb', fontSize: '13px', fontWeight: 700, backgroundColor: 'var(--accent-soft)', padding: '1px 6px', borderRadius: '4px', border: '1px solid var(--border-subtle)' }}>Studio</span>
            </div>
            <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
              Interactive Physics Platform
            </div>
          </div>
        </Link>
      </div>

      {/* 2. Center: Navigation Tabs (Điều tiết ở giữa màn hình) */}
      <nav className="studio-nav-tabs studio-header-center">
        <NavLink
          to="/"
          end
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <HomeIcon size={14} strokeWidth={2.2} />
          <span>Trang chủ</span>
        </NavLink>

        <NavLink
          to="/player"
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <FlaskConical size={14} strokeWidth={2.2} />
          <span>Phòng thí nghiệm (Player)</span>
        </NavLink>

        <NavLink
          to="/dashboard"
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <LayoutDashboard size={14} strokeWidth={2.2} />
          <span>Tổng quan</span>
        </NavLink>

        <NavLink
          to="/teacher/submit"
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <FileEdit size={14} strokeWidth={2.2} />
          <span>Nộp đề & OCR</span>
        </NavLink>

        <NavLink
          to="/library"
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <LibraryIcon size={14} strokeWidth={2.2} />
          <span>Thư viện chuyên đề</span>
        </NavLink>

        <NavLink
          to="/student/assignments"
          className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <Target size={14} strokeWidth={2.2} />
          <span>Thử thách dự đoán</span>
        </NavLink>

        {currentUser?.role === 'ADMIN' && (
          <NavLink
            to="/admin"
            className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#dc2626', fontWeight: 700 }}
          >
            <Settings size={14} strokeWidth={2.2} />
            <span>Quản trị User</span>
          </NavLink>
        )}
      </nav>

      {/* 3. Right Controls: Theme Toggle, User Profile & Actions */}
      <div className="studio-header-right">
        {isAuthenticated && currentUser ? (
          <>
            {/* Theme Toggle Button (Light / Dark) */}
            <button
              onClick={toggleTheme}
              style={{
                padding: '6px 11px',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
                backgroundColor: 'var(--surface-muted)',
                color: isDarkMode ? '#fef08a' : 'var(--text-secondary)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 700,
                transition: 'all 0.15s ease',
              }}
              title={isDarkMode ? 'Chuyển sang chế độ Sáng (Light Mode)' : 'Chuyển sang chế độ Tối (Dark Mode)'}
            >
              <SvgIcon name={isDarkMode ? 'sun' : 'moon'} size={14} color={isDarkMode ? '#fef08a' : '#475569'} />
              <span>{isDarkMode ? 'Sáng' : 'Tối'}</span>
            </button>

            {/* User Info & Avatar */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingLeft: '6px', borderLeft: '1px solid var(--border-subtle)' }}>
              <Link
                to="/profile"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  textDecoration: 'none',
                  cursor: 'pointer',
                  padding: '3px 8px',
                  borderRadius: '8px',
                  transition: 'background-color 0.15s ease',
                }}
                title="Xem & Chỉnh sửa Hồ sơ cá nhân"
                onMouseOver={(e) => (e.currentTarget.style.backgroundColor = 'var(--surface-muted)')}
                onMouseOut={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
              >
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {currentUser.full_name}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2px' }}>
                    <RoleBadge role={currentUser.role} />
                  </div>
                </div>
                <div
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #2563eb, #06b6d4)',
                    color: '#ffffff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 700,
                    fontSize: '14px',
                    boxShadow: '0 2px 8px rgba(37, 99, 235, 0.25)',
                  }}
                >
                  {currentUser.full_name.charAt(0)}
                </div>
              </Link>

              {/* Logout Button */}
              <button
                onClick={handleLogout}
                style={{
                  padding: '6px 12px',
                  borderRadius: '8px',
                  border: isDarkMode ? '1px solid rgba(239, 68, 68, 0.4)' : '1px solid #fecaca',
                  backgroundColor: isDarkMode ? 'rgba(239, 68, 68, 0.15)' : '#fef2f2',
                  color: isDarkMode ? '#f87171' : '#dc2626',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
                title="Đăng xuất khỏi hệ thống"
              >
                <LogOut size={13} strokeWidth={2.2} />
                <span>Đăng xuất</span>
              </button>
            </div>
          </>
        ) : (
          /* Khi CHƯA ĐĂNG NHẬP */
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              onClick={toggleTheme}
              style={{
                padding: '7px 11px',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
                backgroundColor: 'var(--surface-muted)',
                color: isDarkMode ? '#fef08a' : 'var(--text-secondary)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '12px',
                fontWeight: 700,
                transition: 'all 0.15s ease',
              }}
              title={isDarkMode ? 'Chuyển sang chế độ Sáng (Light Mode)' : 'Chuyển sang chế độ Tối (Dark Mode)'}
            >
              <SvgIcon name={isDarkMode ? 'sun' : 'moon'} size={14} color={isDarkMode ? '#fef08a' : '#475569'} />
              <span>{isDarkMode ? 'Sáng' : 'Tối'}</span>
            </button>

            <Link
              to="/login"
              style={{
                padding: '7px 16px',
                borderRadius: '8px',
                border: '1px solid var(--border-subtle)',
                backgroundColor: 'var(--bg-card)',
                color: 'var(--text-primary)',
                fontSize: '13px',
                fontWeight: 700,
                textDecoration: 'none',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <LogIn size={14} strokeWidth={2.2} />
              <span>Đăng nhập</span>
            </Link>
            <Link
              to="/signup"
              style={{
                padding: '7px 16px',
                borderRadius: '8px',
                border: 'none',
                backgroundColor: '#2563eb',
                color: '#ffffff',
                fontSize: '13px',
                fontWeight: 700,
                textDecoration: 'none',
                boxShadow: '0 2px 8px rgba(37, 99, 235, 0.3)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <UserPlus size={14} strokeWidth={2.2} />
              <span>Đăng ký</span>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
};
