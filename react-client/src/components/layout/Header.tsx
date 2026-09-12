import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { RoleType } from '../../types/database';
import { RoleBadge } from '../common/RoleBadge';
import { IS_MOCK_ENABLED } from '../../config/api';

export const Header: React.FC = () => {
  const { currentUser, switchRole } = useAuth();

  const demoRoles: { role: RoleType; label: string; icon: string }[] = [
    { role: 'TEACHER', label: 'Giáo viên', icon: '👨‍🏫' },
    { role: 'STUDENT', label: 'Học sinh', icon: '🎒' },
    { role: 'REVIEWER', label: 'Thẩm định', icon: '🔍' },
    { role: 'ADMIN', label: 'Quản trị', icon: '⚡' },
  ];

  return (
    <header className="studio-header">
      {/* Brand Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        <Link to="/" className="studio-brand">
          <div className="studio-brand-badge">⚛️</div>
          <div>
            <div className="studio-brand-name">PhysLive Studio</div>
            <div style={{ fontSize: '10px', color: '#64748b', fontWeight: 600, letterSpacing: '0.04em' }}>
              INTERACTIVE PHYSICS PLATFORM
            </div>
          </div>
        </Link>

        {/* Central Workspace Switcher Navigation Tabs */}
        <nav className="studio-nav-tabs">
          <NavLink
            to="/player"
            className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          >
            <span>🧪</span>
            <span>Phòng thí nghiệm (Player)</span>
          </NavLink>

          <NavLink
            to="/teacher/submit"
            className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          >
            <span>📝</span>
            <span>Nộp đề & OCR</span>
          </NavLink>

          <NavLink
            to="/library"
            className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          >
            <span>📚</span>
            <span>Thư viện chuyên đề</span>
          </NavLink>

          <NavLink
            to="/student/assignments"
            className={({ isActive }) => `studio-nav-link ${isActive ? 'active' : ''}`}
          >
            <span>🎯</span>
            <span>Thử thách dự đoán</span>
          </NavLink>
        </nav>
      </div>

      {/* Right Controls: Role Switcher & User Profile */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        {IS_MOCK_ENABLED && (
          <span
            style={{
              fontSize: '11px',
              padding: '3px 9px',
              backgroundColor: '#fef3c7',
              color: '#92400e',
              borderRadius: '6px',
              fontWeight: 700,
              border: '1px solid #fde68a',
            }}
          >
            ⚡ MOCK ON
          </span>
        )}

        {/* Quick Role Switcher */}
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
          <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, padding: '0 8px' }}>
            Demo:
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
                  boxShadow: isSelected ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                  transition: 'all 0.15s ease',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <span>{r.icon}</span>
                <span>{r.label}</span>
              </button>
            );
          })}
        </div>

        {/* User Card */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingLeft: '6px', borderLeft: '1px solid #e2e8f0' }}>
          <Link
            to="/profile"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              textDecoration: 'none',
              cursor: 'pointer',
              padding: '2px 6px',
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

          <Link
            to="/login"
            style={{
              padding: '6px 12px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              backgroundColor: '#ffffff',
              color: '#334155',
              fontSize: '12px',
              fontWeight: 600,
              textDecoration: 'none',
              transition: 'all 0.15s ease',
            }}
            title="Xem giao diện Đăng nhập / Đăng xuất"
          >
            Đăng nhập ➔
          </Link>
        </div>
      </div>
    </header>
  );
};
