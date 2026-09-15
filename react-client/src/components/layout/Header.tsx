import React from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { RoleType } from '../../types/database';
import { RoleBadge } from '../common/RoleBadge';
import { IS_MOCK_ENABLED } from '../../config/api';
import {
  Atom,
  FlaskConical,
  FileEdit,
  Library as LibraryIcon,
  Target,
  Settings,
  GraduationCap,
  BookOpen,
  ShieldCheck,
  KeyRound,
  LogOut,
  LogIn,
  UserPlus,
  Zap,
  Home as HomeIcon,
  LayoutDashboard,
} from 'lucide-react';
import { SvgIcon } from '../common/SvgIcon';

export const Header: React.FC = () => {
  const { currentUser, isAuthenticated, switchRole, logout } = useAuth();
  const navigate = useNavigate();

  const [isDarkMode, setIsDarkMode] = React.useState<boolean>(() => {
    return document.documentElement.dataset.theme === 'dark';
  });

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

  const demoRoles: { role: RoleType; label: string; icon: React.ReactNode }[] = [
    { role: 'TEACHER', label: 'Giáo viên', icon: <GraduationCap size={13} strokeWidth={2.5} /> },
    { role: 'STUDENT', label: 'Học sinh', icon: <BookOpen size={13} strokeWidth={2.5} /> },
    { role: 'REVIEWER', label: 'Thẩm định', icon: <ShieldCheck size={13} strokeWidth={2.5} /> },
    { role: 'ADMIN', label: 'Quản trị', icon: <KeyRound size={13} strokeWidth={2.5} /> },
  ];

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
        borderBottom: '1px solid rgba(226, 232, 240, 0.8)',
      }}
    >
      {/* Brand Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
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
            <div className="studio-brand-name" style={{ fontSize: '17px', fontWeight: 800, letterSpacing: '-0.02em', color: '#0f172a' }}>
              PhysLive <span style={{ color: '#2563eb', fontSize: '13px', fontWeight: 700, backgroundColor: '#eff6ff', padding: '1px 6px', borderRadius: '4px', border: '1px solid #bfdbfe' }}>Studio</span>
            </div>
            <div style={{ fontSize: '10px', color: '#64748b', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
              Interactive Physics Platform
            </div>
          </div>
        </Link>

        {/* Central Workspace Switcher Navigation Tabs */}
        <nav className="studio-nav-tabs" style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'rgba(241, 245, 249, 0.85)', padding: '4px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
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
      </div>

      {/* Right Controls: Role Switcher & User Profile */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {IS_MOCK_ENABLED && (
          <span
            style={{
              fontSize: '11px',
              padding: '4px 8px',
              backgroundColor: 'rgba(254, 243, 199, 0.9)',
              color: '#92400e',
              borderRadius: '6px',
              fontWeight: 700,
              border: '1px solid #fde68a',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <Zap size={12} strokeWidth={2.5} />
            <span>MOCK ON</span>
          </span>
        )}

        {isAuthenticated && currentUser ? (
          <>
            {/* Quick Role Switcher khi đã đăng nhập */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                backgroundColor: '#f1f5f9',
                padding: '3px 4px',
                borderRadius: '999px',
                border: '1px solid #e2e8f0',
              }}
            >
              <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700, padding: '0 8px' }}>
                Role:
              </span>
              {demoRoles.map((r) => {
                const isSelected = currentUser.role === r.role;
                return (
                  <button
                    key={r.role}
                    onClick={() => switchRole(r.role)}
                    style={{
                      border: 'none',
                      backgroundColor: isSelected ? '#ffffff' : 'transparent',
                      color: isSelected ? '#2563eb' : '#64748b',
                      padding: '4px 10px',
                      borderRadius: '999px',
                      fontSize: '12px',
                      fontWeight: isSelected ? 700 : 500,
                      cursor: 'pointer',
                      boxShadow: isSelected ? '0 1px 4px rgba(0,0,0,0.08)' : 'none',
                      transition: 'all 0.15s ease',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '5px',
                    }}
                  >
                    <span>{r.icon}</span>
                    <span>{r.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Theme Toggle Button (Light / Dark) */}
            <button
              onClick={toggleTheme}
              style={{
                padding: '6px 11px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                backgroundColor: isDarkMode ? '#1e293b' : '#f8fafc',
                color: isDarkMode ? '#fef08a' : '#475569',
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingLeft: '6px', borderLeft: '1px solid #e2e8f0' }}>
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
                onMouseOver={(e) => (e.currentTarget.style.backgroundColor = '#f1f5f9')}
                onMouseOut={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
              >
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#0f172a' }}>
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
                  border: '1px solid #fecaca',
                  backgroundColor: '#fef2f2',
                  color: '#dc2626',
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
                border: '1px solid #cbd5e1',
                backgroundColor: isDarkMode ? '#1e293b' : '#f8fafc',
                color: isDarkMode ? '#fef08a' : '#475569',
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
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
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
